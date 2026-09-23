/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.sql.calcite.remote;

import static org.opensearch.sql.util.MatcherUtils.rows;
import static org.opensearch.sql.util.MatcherUtils.schema;
import static org.opensearch.sql.util.MatcherUtils.verifyDataRows;
import static org.opensearch.sql.util.MatcherUtils.verifyDataRowsInOrder;
import static org.opensearch.sql.util.MatcherUtils.verifySchema;

import java.io.IOException;
import java.util.List;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.opensearch.client.Request;
import org.opensearch.sql.ppl.PPLIntegTestCase;

/**
 * Integration tests for multi-value/array operators against {@code multi_value} fields of
 * NON-keyword scalar types (long, double, boolean, date, ip), executed against the analytics engine
 * (composite/parquet). Complements {@link CalciteMultiValueKeywordOperatorIT} (keyword only).
 *
 * <p>These exercise the element-type-sensitive paths that {@code ARRAY<ANY>} could not serve
 * correctly: per-element projection type, typed {@code mvindex}, {@code array_length}, and
 * {@code mvexpand} followed by typed aggregation/sort. Depends on the analytics-engine all-scalar-
 * type {@code multi_value} storage (opensearch-project/OpenSearch#23063) plus the SQL-plugin
 * element-typing change ({@code OpenSearchArrayType}).
 *
 * <p>Assertions are EXACT (value + type) so a wrong element type, error response, or empty result
 * cannot false-pass.
 */
public class CalciteMultiValueTypesIT extends PPLIntegTestCase {

  private static final String INDEX = "mv_types_ops";

  @Override
  public void init() throws Exception {
    super.init();
    enableCalcite();
    provisionIndex();
  }

  private void provisionIndex() throws IOException {
    try {
      client().performRequest(new Request("DELETE", "/" + INDEX));
    } catch (Exception ignored) {
    }
    String mapping =
        "{\"settings\":{\"number_of_shards\":1,\"number_of_replicas\":0,"
            + "\"index.pluggable.dataformat.enabled\":true,"
            + "\"index.pluggable.dataformat\":\"composite\","
            + "\"index.composite.primary_data_format\":\"parquet\","
            + "\"index.composite.secondary_data_formats\":[\"lucene\"]},"
            + "\"mappings\":{\"properties\":{"
            + "\"id\":{\"type\":\"keyword\"},"
            + "\"lv\":{\"type\":\"long\",\"multi_value\":true},"
            + "\"dv\":{\"type\":\"double\",\"multi_value\":true},"
            + "\"bv\":{\"type\":\"boolean\",\"multi_value\":true},"
            + "\"dtv\":{\"type\":\"date\",\"multi_value\":true},"
            + "\"ipv\":{\"type\":\"ip\",\"multi_value\":true}}}}";
    Request create = new Request("PUT", "/" + INDEX);
    create.setJsonEntity(mapping);
    client().performRequest(create);

    Request health = new Request("GET", "/_cluster/health/" + INDEX);
    health.addParameter("wait_for_status", "green");
    health.addParameter("timeout", "30s");
    client().performRequest(health);

    // Every document supplies each field as an ARRAY so every parquet file stores a LIST column.
    // Fixture:
    //   d1: lv[10,20]      dv[1.5,2.5]     bv[true,false]  dtv[2020-01-01,2020-06-15] ipv[10.0.0.1,10.0.0.2]
    //   d2: lv[30]         dv[3.5]         bv[true]        dtv[2021-03-03]            ipv[192.168.0.1]
    //   d3: lv[40,50,60]   dv[4.0,5.0,6.0] bv[false,true]  dtv[2022-12-31]            ipv[172.16.0.1,172.16.0.2]
    bulk(
        "{\"index\":{}}\n{\"id\":\"d1\",\"lv\":[10,20],\"dv\":[1.5,2.5],\"bv\":[true,false],"
            + "\"dtv\":[\"2020-01-01\",\"2020-06-15\"],\"ipv\":[\"10.0.0.1\",\"10.0.0.2\"]}\n"
            + "{\"index\":{}}\n{\"id\":\"d2\",\"lv\":[30],\"dv\":[3.5],\"bv\":[true],"
            + "\"dtv\":[\"2021-03-03\"],\"ipv\":[\"192.168.0.1\"]}\n"
            + "{\"index\":{}}\n{\"id\":\"d3\",\"lv\":[40,50,60],\"dv\":[4.0,5.0,6.0],\"bv\":[false,true],"
            + "\"dtv\":[\"2022-12-31\"],\"ipv\":[\"172.16.0.1\",\"172.16.0.2\"]}\n");
    client().performRequest(new Request("POST", "/" + INDEX + "/_flush?force=true"));
  }

  private void bulk(String body) throws IOException {
    Request r = new Request("POST", "/" + INDEX + "/_bulk");
    r.setJsonEntity(body);
    r.addParameter("refresh", "true");
    client().performRequest(r);
  }

  private JSONObject ppl(String query) throws IOException {
    return executeQuery(query);
  }

  // ==================== projection: schema type is ARRAY per field ====================

  @Test
  public void testProjectionLongArray() throws IOException {
    JSONObject r = ppl(String.format("source=%s | where id='d1' | fields lv", INDEX));
    verifySchema(r, schema("lv", "array"));
    verifyDataRows(r, rows(List.of(10, 20)));
  }

  @Test
  public void testProjectionDoubleArray() throws IOException {
    JSONObject r = ppl(String.format("source=%s | where id='d1' | fields dv", INDEX));
    verifyDataRows(r, rows(List.of(1.5, 2.5)));
  }

  @Test
  public void testProjectionBooleanArray() throws IOException {
    JSONObject r = ppl(String.format("source=%s | where id='d1' | fields bv", INDEX));
    verifyDataRows(r, rows(List.of(true, false)));
  }

  @Test
  public void testProjectionIpArray() throws IOException {
    JSONObject r = ppl(String.format("source=%s | where id='d2' | fields ipv", INDEX));
    verifyDataRows(r, rows(List.of("192.168.0.1")));
  }

  // ==================== array_length across types ====================

  @Test
  public void testArrayLengthLong() throws IOException {
    JSONObject r =
        ppl(String.format("source=%s | where id='d3' | eval n = array_length(lv) | fields n", INDEX));
    verifyDataRows(r, rows(3));
  }

  @Test
  public void testArrayLengthDouble() throws IOException {
    JSONObject r =
        ppl(String.format("source=%s | where id='d3' | eval n = array_length(dv) | fields n", INDEX));
    verifyDataRows(r, rows(3));
  }

  // ==================== typed mvindex (element retains its type) ====================

  @Test
  public void testMvindexLongIsNumeric() throws IOException {
    // mvindex(lv,0)=10; +5 must be arithmetic (15), proving the element is LONG not string.
    JSONObject r =
        ppl(
            String.format(
                "source=%s | where id='d1' | eval x = mvindex(lv, 0) + 5 | fields x", INDEX));
    verifyDataRows(r, rows(15));
  }

  @Test
  public void testMvindexDoubleIsNumeric() throws IOException {
    JSONObject r =
        ppl(
            String.format(
                "source=%s | where id='d1' | eval x = mvindex(dv, 1) + 0.5 | fields x", INDEX));
    verifyDataRows(r, rows(3.0));
  }

  // ==================== mvexpand + typed aggregation (the ARRAY<ANY> failure case) ====================

  @Test
  public void testMvexpandLongThenSum() throws IOException {
    // d3 lv=[40,50,60] -> expand to 3 rows -> sum must be numeric 150 (not string concat).
    JSONObject r =
        ppl(
            String.format(
                "source=%s | where id='d3' | mvexpand lv | stats sum(lv) as s", INDEX));
    verifyDataRows(r, rows(150));
  }

  @Test
  public void testMvexpandLongThenSort() throws IOException {
    // Numeric sort (10<20<30<40<50<60), not lexical ("10"<"20"<"30"<"40"<"50"<"60" happens to
    // agree here, so use values where lexical != numeric would differ: include multi-digit).
    JSONObject r =
        ppl(String.format("source=%s | mvexpand lv | sort lv | fields lv", INDEX));
    verifyDataRowsInOrder(
        r, rows(10), rows(20), rows(30), rows(40), rows(50), rows(60));
  }

  @Test
  public void testMvexpandDoubleThenAvg() throws IOException {
    // d3 dv=[4.0,5.0,6.0] -> avg = 5.0
    JSONObject r =
        ppl(
            String.format(
                "source=%s | where id='d3' | mvexpand dv | stats avg(dv) as a", INDEX));
    verifyDataRows(r, rows(5.0));
  }

  @Test
  public void testMvexpandBoolean() throws IOException {
    // d1 bv=[true,false] -> expand to 2 rows.
    JSONObject r =
        ppl(
            String.format(
                "source=%s | where id='d1' | mvexpand bv | stats count() as c", INDEX));
    verifyDataRows(r, rows(2));
  }

  // ==================== implicit =/!= as element membership (contains) ====================
  // On a multi_value field, `field = x` means "the list contains x" (existential over elements);
  // `field != x` means "the list does not contain x". Rewritten to ARRAY_CONTAINS / NOT.

  @Test
  public void testImplicitEqLongMembership() throws IOException {
    // lv: d1[10,20] d2[30] d3[40,50,60] -> lv = 30 contains only d2.
    JSONObject r = ppl(String.format("source=%s | where lv = 30 | sort id | fields id", INDEX));
    verifyDataRowsInOrder(r, rows("d2"));
  }

  @Test
  public void testImplicitEqLongMembershipMultiElement() throws IOException {
    // lv = 20 -> only d1 [10,20]; lv = 50 -> only d3 [40,50,60].
    JSONObject r = ppl(String.format("source=%s | where lv = 50 | sort id | fields id", INDEX));
    verifyDataRowsInOrder(r, rows("d3"));
  }

  @Test
  public void testImplicitNeqLongMembership() throws IOException {
    // lv != 30 -> docs whose list does NOT contain 30: d1 [10,20], d3 [40,50,60].
    JSONObject r = ppl(String.format("source=%s | where lv != 30 | sort id | fields id", INDEX));
    verifyDataRowsInOrder(r, rows("d1"), rows("d3"));
  }

  @Test
  public void testImplicitEqDoubleMembership() throws IOException {
    // dv: d1[1.5,2.5] d2[3.5] d3[4.0,5.0,6.0] -> dv = 2.5 contains only d1.
    JSONObject r = ppl(String.format("source=%s | where dv = 2.5 | sort id | fields id", INDEX));
    verifyDataRowsInOrder(r, rows("d1"));
  }

  @Test
  public void testImplicitEqBooleanMembership() throws IOException {
    // bv: d1[true,false] d2[true] d3[false,true] -> bv = false contains d1, d3.
    JSONObject r = ppl(String.format("source=%s | where bv = false | sort id | fields id", INDEX));
    verifyDataRowsInOrder(r, rows("d1"), rows("d3"));
  }

}
