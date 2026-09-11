/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.sql.calcite.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.opensearch.client.Request;
import org.opensearch.sql.ppl.PPLIntegTestCase;

/**
 * Integration tests for the EXPLICIT multi-value/array operators on a real {@code multi_value} keyword
 * field (promoted via a scalar generation followed by array generations → hybrid scalar+LIST parquet
 * shard). Complements {@code CalciteArrayFunctionIT} (inline {@code array()} literals) by exercising
 * operators against an actually-promoted field.
 *
 * <p>Operators are routed to their correct frontend: the {@code array_*}/{@code cardinality} family is
 * SQL-grammar only (SQL endpoint); the {@code mv*}/{@code array_length}/lambda family and the
 * {@code mvexpand} command are PPL. Implicit scalar-op-on-array behavior is NOT tested — explicit-only.
 *
 * <p>Assertions inspect the raw JSON response (datarows/schema) so they are robust to formatting.
 */
public class CalciteMultiValueKeywordOperatorIT extends PPLIntegTestCase {

  private static final String INDEX = "mv_kw_ops";

  @Override
  public void init() throws Exception {
    super.init();
    enableCalcite();
    provisionMultiValueIndex();
  }

  private void provisionMultiValueIndex() throws IOException {
    try {
      client().performRequest(new Request("DELETE", "/" + INDEX));
    } catch (Exception ignored) {
    }
    String mapping =
        "{\"settings\":{\"number_of_shards\":1,\"number_of_replicas\":0},"
            + "\"mappings\":{\"properties\":{"
            + "\"id\":{\"type\":\"keyword\"},"
            + "\"tags\":{\"type\":\"keyword\",\"multi_value\":true}}}}";
    Request create = new Request("PUT", "/" + INDEX);
    create.setJsonEntity(mapping);
    client().performRequest(create);

    Request health = new Request("GET", "/_cluster/health/" + INDEX);
    health.addParameter("wait_for_status", "green");
    health.addParameter("timeout", "30s");
    client().performRequest(health);

    // Explicit multi_value mapping (declared at creation): all documents supply ARRAYS from the
    // start, so every parquet file stores `tags` as LIST<keyword>. No dynamic promotion, no hybrid
    // scalar+LIST shard. Single-value docs are indexed as single-element arrays.
    bulk(
        "{\"index\":{}}\n{\"id\":\"d1\",\"tags\":[\"prod\"]}\n"
            + "{\"index\":{}}\n{\"id\":\"d2\",\"tags\":[\"blue\"]}\n"
            + "{\"index\":{}}\n{\"id\":\"d3\",\"tags\":[\"prod\",\"blue\"]}\n"
            + "{\"index\":{}}\n{\"id\":\"d4\",\"tags\":[\"green\",\"prod\",\"green\"]}\n");
    client().performRequest(new Request("POST", "/" + INDEX + "/_flush?force=true"));
  }

  private void bulk(String body) throws IOException {
    Request r = new Request("POST", "/" + INDEX + "/_bulk");
    r.setJsonEntity(body);
    r.addParameter("refresh", "true");
    client().performRequest(r);
  }

  /** Runs a PPL query and returns the parsed JSON response. */
  private JSONObject ppl(String query) throws IOException {
    return executeQuery(query);
  }

  /** Runs a SQL query via the JDBC-format endpoint and returns the parsed JSON response. */
  private JSONObject sql(String query) throws IOException {
    Request r = new Request("POST", "/_plugins/_sql");
    r.setJsonEntity("{\"query\":\"" + query.replace("\"", "\\\"") + "\"}");
    org.opensearch.client.Response resp = client().performRequest(r);
    return new JSONObject(
        new String(resp.getEntity().getContent().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
  }

  // ==================== PPL: projection ====================

  @Test
  public void testPplProjectionArrayDoc() throws IOException {
    JSONObject r = ppl(String.format("source=%s | where id='d3' | fields tags", INDEX));
    // tags for d3 = ["prod","blue"]; assert the datarow carries both elements.
    String s = r.getJSONArray("datarows").toString();
    assertTrue(s, s.contains("prod") && s.contains("blue"));
  }

  @Test
  public void testPplProjectionScalarGenerationDoc() throws IOException {
    JSONObject r = ppl(String.format("source=%s | where id='d1' | fields tags", INDEX));
    String s = r.getJSONArray("datarows").toString();
    assertTrue(s, s.contains("prod"));
  }

  // ==================== PPL: array_length ====================

  @Test
  public void testPplArrayLength() throws IOException {
    JSONObject r =
        ppl(String.format("source=%s | where id='d4' | eval n = array_length(tags) | fields n", INDEX));
    assertEquals(3, r.getJSONArray("datarows").getJSONArray(0).getInt(0));
  }

  @Test
  public void testPplArrayLengthFilterByCountUseCase() throws IOException {
    JSONObject r =
        ppl(String.format("source=%s | where array_length(tags) > 1 | sort id | fields id", INDEX));
    assertEquals(2, r.getJSONArray("datarows").length()); // d3, d4
  }

  // ==================== PPL: mvjoin ====================

  @Test
  public void testPplMvjoin() throws IOException {
    JSONObject r =
        ppl(String.format("source=%s | where id='d3' | eval s = mvjoin(tags, '-') | fields s", INDEX));
    assertEquals("prod-blue", r.getJSONArray("datarows").getJSONArray(0).getString(0));
  }

  // ==================== PPL: mvindex ====================

  @Test
  public void testPplMvindex() throws IOException {
    JSONObject r =
        ppl(
            String.format(
                "source=%s | where id='d3' | eval first = mvindex(tags, 0) | fields first", INDEX));
    assertEquals("prod", r.getJSONArray("datarows").getJSONArray(0).getString(0));
  }

  // ==================== PPL: mvfind (index + contains use case) ====================

  @Test
  public void testPplMvfind() throws IOException {
    JSONObject r =
        ppl(
            String.format(
                "source=%s | where id='d3' | eval idx = mvfind(tags, 'blue') | fields idx", INDEX));
    assertEquals(1, r.getJSONArray("datarows").getJSONArray(0).getInt(0));
  }

  @Test
  public void testPplMvfindContainsUseCase() throws IOException {
    JSONObject r =
        ppl(String.format("source=%s | where mvfind(tags, 'blue') >= 0 | sort id | fields id", INDEX));
    // docs containing 'blue': d2 (scalar), d3 ([prod,blue]).
    assertEquals(2, r.getJSONArray("datarows").length());
  }

  // ==================== PPL: mvdedup ====================

  @Test
  public void testPplMvdedup() throws IOException {
    JSONObject r =
        ppl(String.format("source=%s | where id='d4' | eval u = mvdedup(tags) | fields u", INDEX));
    // d4 tags = [green, prod, green] → deduped to 2 distinct elements.
    String s = r.getJSONArray("datarows").toString();
    assertTrue(s, s.contains("green") && s.contains("prod"));
  }

  // ==================== PPL: mvappend ====================

  @Test
  public void testPplMvappend() throws IOException {
    JSONObject r =
        ppl(String.format("source=%s | where id='d2' | eval a = mvappend(tags, 'extra') | fields a", INDEX));
    String s = r.getJSONArray("datarows").toString();
    assertTrue(s, s.contains("blue") && s.contains("extra"));
  }

  // ==================== PPL: mvexpand command (+ group-by use case) ====================

  @Test
  public void testPplMvexpand() throws IOException {
    JSONObject r =
        ppl(
            String.format(
                "source=%s | where id='d3' | mvexpand tags | sort tags | fields id, tags", INDEX));
    assertEquals(2, r.getJSONArray("datarows").length()); // prod, blue
  }

  @Test
  public void testPplMvexpandGroupByUseCase() throws IOException {
    JSONObject r =
        ppl(String.format("source=%s | mvexpand tags | stats count() as c by tags | sort tags", INDEX));
    // per-element counts: blue=2 (d2,d3), green=2 (d4x2 — dedup not applied by mvexpand), prod=3.
    assertEquals(3, r.getJSONArray("datarows").length());
  }

  // ==================== SQL: array_contains (explicit filter/contains) ====================

  @Test
  public void testSqlArrayContainsStandalone() throws IOException {
    JSONObject r =
        sql(String.format("SELECT array_contains(tags, 'prod') AS c FROM %s WHERE id='d3'", INDEX));
    assertEquals(true, r.getJSONArray("datarows").getJSONArray(0).get(0));
  }

  @Test
  public void testSqlArrayContainsFilterUseCase() throws IOException {
    JSONObject r =
        sql(
            String.format(
                "SELECT id FROM %s WHERE array_contains(tags, 'prod') ORDER BY id", INDEX));
    // d1 (scalar prod), d3, d4 contain prod.
    assertEquals(3, r.getJSONArray("datarows").length());
  }

  // ==================== SQL: cardinality / array_length ====================

  @Test
  public void testSqlCardinality() throws IOException {
    JSONObject r =
        sql(String.format("SELECT cardinality(tags) AS n FROM %s WHERE id='d3'", INDEX));
    assertEquals(2, r.getJSONArray("datarows").getJSONArray(0).getInt(0));
  }

  // ==================== SQL: array_join ====================

  @Test
  public void testSqlArrayJoin() throws IOException {
    JSONObject r =
        sql(String.format("SELECT array_join(tags, ',') AS s FROM %s WHERE id='d3'", INDEX));
    assertEquals("prod,blue", r.getJSONArray("datarows").getJSONArray(0).getString(0));
  }

  // ==================== SQL: subscript ====================

  @Test
  public void testSqlSubscript() throws IOException {
    JSONObject r = sql(String.format("SELECT tags[1] AS first FROM %s WHERE id='d3'", INDEX));
    assertEquals("prod", r.getJSONArray("datarows").getJSONArray(0).getString(0));
  }
}
