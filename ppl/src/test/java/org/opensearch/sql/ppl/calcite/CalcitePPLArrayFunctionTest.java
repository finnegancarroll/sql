/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.sql.ppl.calcite;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.test.CalciteAssert;
import org.junit.Test;

public class CalcitePPLArrayFunctionTest extends CalcitePPLAbstractTest {

  public CalcitePPLArrayFunctionTest() {
    super(CalciteAssert.SchemaSpec.SCOTT_WITH_TEMPORAL);
  }

  @Test
  public void testMvjoinWithStringArray() {
    String ppl =
        "source=EMP | eval joined = mvjoin(array('a', 'b', 'c'), ',') | head 1 | fields joined";
    RelNode root = getRelNode(ppl);

    String expectedLogical =
        "LogicalProject(joined=[$8])\n"
            + "  LogicalSort(fetch=[1])\n"
            + "    LogicalProject(EMPNO=[$0], ENAME=[$1], JOB=[$2], MGR=[$3], HIREDATE=[$4],"
            + " SAL=[$5], COMM=[$6], DEPTNO=[$7], joined=[ARRAY_JOIN(array('a', 'b', 'c'), ',')])\n"
            + "      LogicalTableScan(table=[[scott, EMP]])\n";
    verifyLogical(root, expectedLogical);

    String expectedResult = "joined=a,b,c\n";
    verifyResult(root, expectedResult);

    String expectedSparkSql =
        "SELECT ARRAY_JOIN(ARRAY('a', 'b', 'c'), ',') `joined`\n"
            + "FROM `scott`.`EMP`\n"
            + "LIMIT 1";
    verifyPPLToSparkSQL(root, expectedSparkSql);
  }

  @Test
  public void testMvjoinWithDifferentDelimiter() {
    String ppl =
        "source=EMP | eval joined = mvjoin(array('apple', 'banana', 'cherry'), ' | ') | head 1 |"
            + " fields joined";
    RelNode root = getRelNode(ppl);

    String expectedLogical =
        "LogicalProject(joined=[$8])\n"
            + "  LogicalSort(fetch=[1])\n"
            + "    LogicalProject(EMPNO=[$0], ENAME=[$1], JOB=[$2], MGR=[$3], HIREDATE=[$4],"
            + " SAL=[$5], COMM=[$6], DEPTNO=[$7], joined=[ARRAY_JOIN(array('apple':VARCHAR,"
            + " 'banana':VARCHAR, 'cherry':VARCHAR), ' | ':VARCHAR)])\n"
            + "      LogicalTableScan(table=[[scott, EMP]])\n";
    verifyLogical(root, expectedLogical);

    String expectedResult = "joined=apple | banana | cherry\n";
    verifyResult(root, expectedResult);

    String expectedSparkSql =
        "SELECT ARRAY_JOIN(ARRAY('apple', 'banana', 'cherry'), ' | ') `joined`\n"
            + "FROM `scott`.`EMP`\n"
            + "LIMIT 1";
    verifyPPLToSparkSQL(root, expectedSparkSql);
  }

  @Test
  public void testMvjoinWithEmptyArray() {
    String ppl = "source=EMP | eval joined = mvjoin(array(), ',') | head 1 | fields joined";
    RelNode root = getRelNode(ppl);

    String expectedLogical =
        "LogicalProject(joined=[$8])\n"
            + "  LogicalSort(fetch=[1])\n"
            + "    LogicalProject(EMPNO=[$0], ENAME=[$1], JOB=[$2], MGR=[$3], HIREDATE=[$4],"
            + " SAL=[$5], COMM=[$6], DEPTNO=[$7], joined=[ARRAY_JOIN(array(), ',')])\n"
            + "      LogicalTableScan(table=[[scott, EMP]])\n";
    verifyLogical(root, expectedLogical);

    String expectedResult = "joined=\n";
    verifyResult(root, expectedResult);

    String expectedSparkSql =
        "SELECT ARRAY_JOIN(ARRAY(), ',') `joined`\n" + "FROM `scott`.`EMP`\n" + "LIMIT 1";
    verifyPPLToSparkSQL(root, expectedSparkSql);
  }

  @Test
  public void testMvjoinWithFieldReference() {
    String ppl =
        "source=EMP | eval joined = mvjoin(array(ENAME, JOB), '-') | head 1 | fields joined";
    RelNode root = getRelNode(ppl);

    String expectedLogical =
        "LogicalProject(joined=[$8])\n"
            + "  LogicalSort(fetch=[1])\n"
            + "    LogicalProject(EMPNO=[$0], ENAME=[$1], JOB=[$2], MGR=[$3], HIREDATE=[$4],"
            + " SAL=[$5], COMM=[$6], DEPTNO=[$7], joined=[ARRAY_JOIN(array($1, $2), '-')])\n"
            + "      LogicalTableScan(table=[[scott, EMP]])\n";
    verifyLogical(root, expectedLogical);

    String expectedSparkSql =
        "SELECT ARRAY_JOIN(ARRAY(`ENAME`, `JOB`), '-') `joined`\n"
            + "FROM `scott`.`EMP`\n"
            + "LIMIT 1";
    verifyPPLToSparkSQL(root, expectedSparkSql);
  }

  @Test
  public void testMvindexSingleElementPositive() {
    String ppl =
        "source=EMP | eval arr = array('a', 'b', 'c'), result = mvindex(arr, 1) | head 1 |"
            + " fields result";
    RelNode root = getRelNode(ppl);

    String expectedLogical =
        "LogicalProject(result=[$9])\n"
            + "  LogicalSort(fetch=[1])\n"
            + "    LogicalProject(EMPNO=[$0], ENAME=[$1], JOB=[$2], MGR=[$3], HIREDATE=[$4],"
            + " SAL=[$5], COMM=[$6], DEPTNO=[$7], arr=[array('a', 'b', 'c')],"
            + " result=[ITEM(array('a', 'b', 'c'), +(1, 1))])\n"
            + "      LogicalTableScan(table=[[scott, EMP]])\n";
    verifyLogical(root, expectedLogical);

    String expectedResult = "result=b\n";
    verifyResult(root, expectedResult);

    String expectedSparkSql =
        "SELECT ARRAY('a', 'b', 'c')[1 + 1] `result`\n" + "FROM `scott`.`EMP`\n" + "LIMIT 1";
    verifyPPLToSparkSQL(root, expectedSparkSql);
  }

  @Test
  public void testMvindexSingleElementNegative() {
    String ppl =
        "source=EMP | eval arr = array('a', 'b', 'c'), result = mvindex(arr, -1) | head 1 |"
            + " fields result";
    RelNode root = getRelNode(ppl);

    String expectedLogical =
        "LogicalProject(result=[$9])\n"
            + "  LogicalSort(fetch=[1])\n"
            + "    LogicalProject(EMPNO=[$0], ENAME=[$1], JOB=[$2], MGR=[$3], HIREDATE=[$4],"
            + " SAL=[$5], COMM=[$6], DEPTNO=[$7], arr=[array('a', 'b', 'c')],"
            + " result=[ITEM(array('a', 'b', 'c'), +(+(ARRAY_LENGTH(array('a', 'b', 'c')),"
            + " -1), 1))])\n"
            + "      LogicalTableScan(table=[[scott, EMP]])\n";
    verifyLogical(root, expectedLogical);

    String expectedResult = "result=c\n";
    verifyResult(root, expectedResult);

    String expectedSparkSql =
        "SELECT ARRAY('a', 'b', 'c')[ARRAY_LENGTH(ARRAY('a', 'b', 'c')) + -1 + 1]"
            + " `result`\n"
            + "FROM `scott`.`EMP`\n"
            + "LIMIT 1";
    verifyPPLToSparkSQL(root, expectedSparkSql);
  }

  @Test
  public void testMvindexRangePositive() {
    String ppl =
        "source=EMP | eval arr = array(1, 2, 3, 4, 5), result = mvindex(arr, 1, 3) | head 1 |"
            + " fields result";
    RelNode root = getRelNode(ppl);

    String expectedLogical =
        "LogicalProject(result=[$9])\n"
            + "  LogicalSort(fetch=[1])\n"
            + "    LogicalProject(EMPNO=[$0], ENAME=[$1], JOB=[$2], MGR=[$3], HIREDATE=[$4],"
            + " SAL=[$5], COMM=[$6], DEPTNO=[$7], arr=[array(1, 2, 3, 4, 5)],"
            + " result=[ARRAY_SLICE(array(1, 2, 3, 4, 5), 1, +(-(3, 1), 1))])\n"
            + "      LogicalTableScan(table=[[scott, EMP]])\n";
    verifyLogical(root, expectedLogical);

    String expectedResult = "result=[2, 3, 4]\n";
    verifyResult(root, expectedResult);

    String expectedSparkSql =
        "SELECT ARRAY_SLICE(ARRAY(1, 2, 3, 4, 5), 1, 3 - 1 + 1) `result`\n"
            + "FROM `scott`.`EMP`\n"
            + "LIMIT 1";
    verifyPPLToSparkSQL(root, expectedSparkSql);
  }

  @Test
  public void testMvindexRangeNegative() {
    String ppl =
        "source=EMP | eval arr = array(1, 2, 3, 4, 5), result = mvindex(arr, -3, -1) | head 1 |"
            + " fields result";
    RelNode root = getRelNode(ppl);

    String expectedLogical =
        "LogicalProject(result=[$9])\n"
            + "  LogicalSort(fetch=[1])\n"
            + "    LogicalProject(EMPNO=[$0], ENAME=[$1], JOB=[$2], MGR=[$3], HIREDATE=[$4],"
            + " SAL=[$5], COMM=[$6], DEPTNO=[$7], arr=[array(1, 2, 3, 4, 5)],"
            + " result=[ARRAY_SLICE(array(1, 2, 3, 4, 5), +(ARRAY_LENGTH(array(1, 2, 3, 4, 5)),"
            + " -3), +(-(+(ARRAY_LENGTH(array(1, 2, 3, 4, 5)), -1),"
            + " +(ARRAY_LENGTH(array(1, 2, 3, 4, 5)), -3)), 1))])\n"
            + "      LogicalTableScan(table=[[scott, EMP]])\n";
    verifyLogical(root, expectedLogical);

    String expectedResult = "result=[3, 4, 5]\n";
    verifyResult(root, expectedResult);

    String expectedSparkSql =
        "SELECT ARRAY_SLICE(ARRAY(1, 2, 3, 4, 5), ARRAY_LENGTH(ARRAY(1, 2, 3, 4, 5)) + -3,"
            + " ARRAY_LENGTH(ARRAY(1, 2, 3, 4, 5)) + -1 - (ARRAY_LENGTH(ARRAY(1, 2, 3, 4, 5))"
            + " + -3) + 1) `result`\n"
            + "FROM `scott`.`EMP`\n"
            + "LIMIT 1";
    verifyPPLToSparkSQL(root, expectedSparkSql);
  }

  @Test
  public void testMvfindWithMatch() {
    String ppl =
        "source=EMP | eval arr = array('apple', 'banana', 'apricot'), result = mvfind(arr,"
            + " 'ban.*') | head 1 | fields result";
    RelNode root = getRelNode(ppl);

    String expectedResult = "result=1\n";
    verifyResult(root, expectedResult);

    String expectedSparkSql =
        "SELECT MVFIND(ARRAY('apple', 'banana', 'apricot'), 'ban.*') `result`\n"
            + "FROM `scott`.`EMP`\n"
            + "LIMIT 1";
    verifyPPLToSparkSQL(root, expectedSparkSql);
  }

  @Test
  public void testMvfindWithNoMatch() {
    String ppl =
        "source=EMP | eval arr = array('cat', 'dog', 'bird'), result = mvfind(arr, 'fish') | head"
            + " 1 | fields result";
    RelNode root = getRelNode(ppl);

    String expectedResult = "result=null\n";
    verifyResult(root, expectedResult);

    String expectedSparkSql =
        "SELECT MVFIND(ARRAY('cat', 'dog', 'bird'), 'fish') `result`\n"
            + "FROM `scott`.`EMP`\n"
            + "LIMIT 1";
    verifyPPLToSparkSQL(root, expectedSparkSql);
  }

  @Test
  public void testMvfindWithFirstMatch() {
    String ppl =
        "source=EMP | eval arr = array('error123', 'info', 'error456'), result = mvfind(arr,"
            + " 'err.*') | head 1 | fields result";
    RelNode root = getRelNode(ppl);

    String expectedResult = "result=0\n";
    verifyResult(root, expectedResult);

    String expectedSparkSql =
        "SELECT MVFIND(ARRAY('error123', 'info', 'error456'), 'err.*') `result`\n"
            + "FROM `scott`.`EMP`\n"
            + "LIMIT 1";
    verifyPPLToSparkSQL(root, expectedSparkSql);
  }

  @Test
  public void testMvfindWithMultipleMatches() {
    String ppl =
        "source=EMP | eval arr = array('test1', 'test2', 'test3'), result = mvfind(arr, 'test.*')"
            + " | head 1 | fields result";
    RelNode root = getRelNode(ppl);

    String expectedResult = "result=0\n";
    verifyResult(root, expectedResult);

    String expectedSparkSql =
        "SELECT MVFIND(ARRAY('test1', 'test2', 'test3'), 'test.*') `result`\n"
            + "FROM `scott`.`EMP`\n"
            + "LIMIT 1";
    verifyPPLToSparkSQL(root, expectedSparkSql);
  }

  @Test
  public void testMvfindWithComplexRegex() {
    String ppl =
        "source=EMP | eval arr = array('abc123', 'def456', 'ghi789'), result = mvfind(arr,"
            + " 'def\\d+') | head 1 | fields result";
    RelNode root = getRelNode(ppl);

    String expectedResult = "result=1\n";
    verifyResult(root, expectedResult);

    String expectedSparkSql =
        "SELECT MVFIND(ARRAY('abc123', 'def456', 'ghi789'), 'def\\d+') `result`\n"
            + "FROM `scott`.`EMP`\n"
            + "LIMIT 1";
    verifyPPLToSparkSQL(root, expectedSparkSql);
  }

  @Test
  public void testMvdedupWithDuplicates() {
    String ppl =
        "source=EMP | eval arr = array(1, 2, 2, 3, 1, 4), result = mvdedup(arr) | head 1 |"
            + " fields result";
    RelNode root = getRelNode(ppl);

    String expectedLogical =
        "LogicalProject(result=[$9])\n"
            + "  LogicalSort(fetch=[1])\n"
            + "    LogicalProject(EMPNO=[$0], ENAME=[$1], JOB=[$2], MGR=[$3], HIREDATE=[$4],"
            + " SAL=[$5], COMM=[$6], DEPTNO=[$7], arr=[array(1, 2, 2, 3, 1, 4)],"
            + " result=[ARRAY_DISTINCT(array(1, 2, 2, 3, 1, 4))])\n"
            + "      LogicalTableScan(table=[[scott, EMP]])\n";
    verifyLogical(root, expectedLogical);

    String expectedResult = "result=[1, 2, 3, 4]\n";
    verifyResult(root, expectedResult);

    String expectedSparkSql =
        "SELECT ARRAY_DISTINCT(ARRAY(1, 2, 2, 3, 1, 4)) `result`\n"
            + "FROM `scott`.`EMP`\n"
            + "LIMIT 1";
    verifyPPLToSparkSQL(root, expectedSparkSql);
  }

  @Test
  public void testMvdedupWithNoDuplicates() {
    String ppl =
        "source=EMP | eval arr = array(1, 2, 3, 4), result = mvdedup(arr) | head 1 | fields"
            + " result";
    RelNode root = getRelNode(ppl);

    String expectedLogical =
        "LogicalProject(result=[$9])\n"
            + "  LogicalSort(fetch=[1])\n"
            + "    LogicalProject(EMPNO=[$0], ENAME=[$1], JOB=[$2], MGR=[$3], HIREDATE=[$4],"
            + " SAL=[$5], COMM=[$6], DEPTNO=[$7], arr=[array(1, 2, 3, 4)],"
            + " result=[ARRAY_DISTINCT(array(1, 2, 3, 4))])\n"
            + "      LogicalTableScan(table=[[scott, EMP]])\n";
    verifyLogical(root, expectedLogical);

    String expectedResult = "result=[1, 2, 3, 4]\n";
    verifyResult(root, expectedResult);

    String expectedSparkSql =
        "SELECT ARRAY_DISTINCT(ARRAY(1, 2, 3, 4)) `result`\n" + "FROM `scott`.`EMP`\n" + "LIMIT 1";
    verifyPPLToSparkSQL(root, expectedSparkSql);
  }

  @Test
  public void testMvdedupPreservesOrder() {
    String ppl =
        "source=EMP | eval arr = array('z', 'a', 'z', 'b', 'a', 'c'), result = mvdedup(arr) |"
            + " head 1 | fields result";
    RelNode root = getRelNode(ppl);

    String expectedLogical =
        "LogicalProject(result=[$9])\n"
            + "  LogicalSort(fetch=[1])\n"
            + "    LogicalProject(EMPNO=[$0], ENAME=[$1], JOB=[$2], MGR=[$3], HIREDATE=[$4],"
            + " SAL=[$5], COMM=[$6], DEPTNO=[$7], arr=[array('z', 'a', 'z', 'b', 'a', 'c')],"
            + " result=[ARRAY_DISTINCT(array('z', 'a', 'z', 'b', 'a', 'c'))])\n"
            + "      LogicalTableScan(table=[[scott, EMP]])\n";
    verifyLogical(root, expectedLogical);

    String expectedResult = "result=[z, a, b, c]\n";
    verifyResult(root, expectedResult);

    String expectedSparkSql =
        "SELECT ARRAY_DISTINCT(ARRAY('z', 'a', 'z', 'b', 'a', 'c')) `result`\n"
            + "FROM `scott`.`EMP`\n"
            + "LIMIT 1";
    verifyPPLToSparkSQL(root, expectedSparkSql);
  }

  @Test
  public void testMvzipBasic() {
    String ppl =
        "source=EMP | eval arr1 = array('a', 'b', 'c'), arr2 = array('x', 'y'), result ="
            + " mvzip(arr1, arr2) | head 1 | fields result";
    RelNode root = getRelNode(ppl);

    String expectedLogical =
        "LogicalProject(result=[$10])\n"
            + "  LogicalSort(fetch=[1])\n"
            + "    LogicalProject(EMPNO=[$0], ENAME=[$1], JOB=[$2], MGR=[$3], HIREDATE=[$4],"
            + " SAL=[$5], COMM=[$6], DEPTNO=[$7], arr1=[array('a', 'b', 'c')], arr2=[array('x',"
            + " 'y')], result=[mvzip(array('a', 'b', 'c'), array('x', 'y'))])\n"
            + "      LogicalTableScan(table=[[scott, EMP]])\n";
    verifyLogical(root, expectedLogical);

    String expectedResult = "result=[a,x, b,y]\n";
    verifyResult(root, expectedResult);

    String expectedSparkSql =
        "SELECT MVZIP(ARRAY('a', 'b', 'c'), ARRAY('x', 'y')) `result`\n"
            + "FROM `scott`.`EMP`\n"
            + "LIMIT 1";
    verifyPPLToSparkSQL(root, expectedSparkSql);
  }

  @Test
  public void testMvzipWithCustomDelimiter() {
    String ppl =
        "source=EMP | eval result = mvzip(array('a', 'b'), array('x', 'y'), '|') | head 1 |"
            + " fields result";
    RelNode root = getRelNode(ppl);

    String expectedLogical =
        "LogicalProject(result=[$8])\n"
            + "  LogicalSort(fetch=[1])\n"
            + "    LogicalProject(EMPNO=[$0], ENAME=[$1], JOB=[$2], MGR=[$3], HIREDATE=[$4],"
            + " SAL=[$5], COMM=[$6], DEPTNO=[$7], result=[mvzip(array('a', 'b'), array('x', 'y'),"
            + " '|')])\n"
            + "      LogicalTableScan(table=[[scott, EMP]])\n";
    verifyLogical(root, expectedLogical);

    String expectedResult = "result=[a|x, b|y]\n";
    verifyResult(root, expectedResult);

    String expectedSparkSql =
        "SELECT MVZIP(ARRAY('a', 'b'), ARRAY('x', 'y'), '|') `result`\n"
            + "FROM `scott`.`EMP`\n"
            + "LIMIT 1";
    verifyPPLToSparkSQL(root, expectedSparkSql);
  }

  @Test
  public void testMvzipNested() {
    String ppl =
        "source=EMP | eval field1 = array('a', 'b'), field2 = array('c', 'd'), field3 ="
            + " array('e', 'f'), result = mvzip(mvzip(field1, field2, '|'), field3, '|') | head 1"
            + " | fields result";
    RelNode root = getRelNode(ppl);

    String expectedLogical =
        "LogicalProject(result=[$11])\n"
            + "  LogicalSort(fetch=[1])\n"
            + "    LogicalProject(EMPNO=[$0], ENAME=[$1], JOB=[$2], MGR=[$3], HIREDATE=[$4],"
            + " SAL=[$5], COMM=[$6], DEPTNO=[$7], field1=[array('a', 'b')], field2=[array('c',"
            + " 'd')], field3=[array('e', 'f')], result=[mvzip(mvzip(array('a', 'b'), array('c',"
            + " 'd'), '|'), array('e', 'f'), '|')])\n"
            + "      LogicalTableScan(table=[[scott, EMP]])\n";
    verifyLogical(root, expectedLogical);

    String expectedResult = "result=[a|c|e, b|d|f]\n";
    verifyResult(root, expectedResult);

    String expectedSparkSql =
        "SELECT MVZIP(MVZIP(ARRAY('a', 'b'), ARRAY('c', 'd'), '|'), ARRAY('e', 'f'),"
            + " '|') `result`\n"
            + "FROM `scott`.`EMP`\n"
            + "LIMIT 1";
    verifyPPLToSparkSQL(root, expectedSparkSql);
  }

  @Test
  public void testMvzipWithEmptyLeftArray() {
    String ppl =
        "source=EMP | eval result = mvzip(array(), array('a', 'b')) | head 1 | fields result";
    RelNode root = getRelNode(ppl);

    String expectedLogical =
        "LogicalProject(result=[$8])\n"
            + "  LogicalSort(fetch=[1])\n"
            + "    LogicalProject(EMPNO=[$0], ENAME=[$1], JOB=[$2], MGR=[$3], HIREDATE=[$4],"
            + " SAL=[$5], COMM=[$6], DEPTNO=[$7], result=[mvzip(array(), array('a', 'b'))])\n"
            + "      LogicalTableScan(table=[[scott, EMP]])\n";
    verifyLogical(root, expectedLogical);

    String expectedSparkSql =
        "SELECT MVZIP(ARRAY(), ARRAY('a', 'b')) `result`\n" + "FROM `scott`.`EMP`\n" + "LIMIT 1";
    verifyPPLToSparkSQL(root, expectedSparkSql);
  }

  @Test
  public void testMvzipWithEmptyRightArray() {
    String ppl =
        "source=EMP | eval result = mvzip(array('a', 'b'), array()) | head 1 | fields result";
    RelNode root = getRelNode(ppl);

    String expectedLogical =
        "LogicalProject(result=[$8])\n"
            + "  LogicalSort(fetch=[1])\n"
            + "    LogicalProject(EMPNO=[$0], ENAME=[$1], JOB=[$2], MGR=[$3], HIREDATE=[$4],"
            + " SAL=[$5], COMM=[$6], DEPTNO=[$7], result=[mvzip(array('a', 'b'), array())])\n"
            + "      LogicalTableScan(table=[[scott, EMP]])\n";
    verifyLogical(root, expectedLogical);

    String expectedSparkSql =
        "SELECT MVZIP(ARRAY('a', 'b'), ARRAY()) `result`\n" + "FROM `scott`.`EMP`\n" + "LIMIT 1";
    verifyPPLToSparkSQL(root, expectedSparkSql);
  }

  @Test
  public void testMvzipWithBothEmptyArrays() {
    String ppl = "source=EMP | eval result = mvzip(array(), array()) | head 1 | fields result";
    RelNode root = getRelNode(ppl);

    String expectedLogical =
        "LogicalProject(result=[$8])\n"
            + "  LogicalSort(fetch=[1])\n"
            + "    LogicalProject(EMPNO=[$0], ENAME=[$1], JOB=[$2], MGR=[$3], HIREDATE=[$4],"
            + " SAL=[$5], COMM=[$6], DEPTNO=[$7], result=[mvzip(array(), array())])\n"
            + "      LogicalTableScan(table=[[scott, EMP]])\n";
    verifyLogical(root, expectedLogical);

    String expectedSparkSql =
        "SELECT MVZIP(ARRAY(), ARRAY()) `result`\n" + "FROM `scott`.`EMP`\n" + "LIMIT 1";
    verifyPPLToSparkSQL(root, expectedSparkSql);
  }

  @Test
  public void testSplitWithSemicolonDelimiter() {
    String ppl =
        "source=EMP | eval test = 'buttercup;rarity;tenderhoof', result = split(test, ';') | head"
            + " 1 | fields result";
    RelNode root = getRelNode(ppl);

    String expectedLogical =
        "LogicalProject(result=[$9])\n"
            + "  LogicalSort(fetch=[1])\n"
            + "    LogicalProject(EMPNO=[$0], ENAME=[$1], JOB=[$2], MGR=[$3], HIREDATE=[$4],"
            + " SAL=[$5], COMM=[$6], DEPTNO=[$7], test=['buttercup;rarity;tenderhoof':VARCHAR],"
            + " result=[CASE(=(';', ''),"
            + " REGEXP_EXTRACT_ALL('buttercup;rarity;tenderhoof':VARCHAR, '.'),"
            + " SPLIT('buttercup;rarity;tenderhoof':VARCHAR, ';'))])\n"
            + "      LogicalTableScan(table=[[scott, EMP]])\n";
    verifyLogical(root, expectedLogical);

    String expectedSparkSql =
        "SELECT CASE WHEN ';' = '' THEN REGEXP_EXTRACT_ALL('buttercup;rarity;tenderhoof', "
            + "'.') ELSE SPLIT('buttercup;rarity;tenderhoof', ';') END "
            + "`result`\n"
            + "FROM `scott`.`EMP`\n"
            + "LIMIT 1";
    verifyPPLToSparkSQL(root, expectedSparkSql);
  }

  @Test
  public void testSplitWithMultiCharDelimiter() {
    String ppl =
        "source=EMP | eval test = '1a2b3c4def567890', result = split(test, 'def') | head 1 |"
            + " fields result";
    RelNode root = getRelNode(ppl);

    String expectedLogical =
        "LogicalProject(result=[$9])\n"
            + "  LogicalSort(fetch=[1])\n"
            + "    LogicalProject(EMPNO=[$0], ENAME=[$1], JOB=[$2], MGR=[$3], HIREDATE=[$4],"
            + " SAL=[$5], COMM=[$6], DEPTNO=[$7], test=['1a2b3c4def567890':VARCHAR],"
            + " result=[CASE(=('def':VARCHAR, ''), REGEXP_EXTRACT_ALL('1a2b3c4def567890':VARCHAR,"
            + " '.'), SPLIT('1a2b3c4def567890':VARCHAR, 'def':VARCHAR))])\n"
            + "      LogicalTableScan(table=[[scott, EMP]])\n";
    verifyLogical(root, expectedLogical);

    String expectedSparkSql =
        "SELECT CASE WHEN 'def' = '' THEN REGEXP_EXTRACT_ALL('1a2b3c4def567890', "
            + "'.') ELSE SPLIT('1a2b3c4def567890', 'def') END `result`\n"
            + "FROM `scott`.`EMP`\n"
            + "LIMIT 1";
    verifyPPLToSparkSQL(root, expectedSparkSql);
  }

  @Test
  public void testSplitWithEmptyDelimiter() {
    String ppl =
        "source=EMP | eval test = 'abcd', result = split(test, '') | head 1 | fields result";
    RelNode root = getRelNode(ppl);

    // With empty delimiter, should split into individual characters
    String expectedLogical =
        "LogicalProject(result=[$9])\n"
            + "  LogicalSort(fetch=[1])\n"
            + "    LogicalProject(EMPNO=[$0], ENAME=[$1], JOB=[$2], MGR=[$3], HIREDATE=[$4],"
            + " SAL=[$5], COMM=[$6], DEPTNO=[$7], test=['abcd':VARCHAR],"
            + " result=[CASE(=('':VARCHAR, ''), REGEXP_EXTRACT_ALL('abcd':VARCHAR,"
            + " '.'), SPLIT('abcd':VARCHAR, '':VARCHAR))])\n"
            + "      LogicalTableScan(table=[[scott, EMP]])\n";
    verifyLogical(root, expectedLogical);

    String expectedSparkSql =
        "SELECT CASE WHEN '' = '' THEN REGEXP_EXTRACT_ALL('abcd', '.') "
            + "ELSE SPLIT('abcd', '') END `result`\n"
            + "FROM `scott`.`EMP`\n"
            + "LIMIT 1";
    verifyPPLToSparkSQL(root, expectedSparkSql);
  }

  @Test
  public void testMvmapBasic() {
    String ppl =
        "source=EMP | eval arr = array(1, 2, 3), result = mvmap(arr, arr * 10) | head 1 |"
            + " fields result";
    RelNode root = getRelNode(ppl);

    String expectedLogical =
        "LogicalProject(result=[$9])\n"
            + "  LogicalSort(fetch=[1])\n"
            + "    LogicalProject(EMPNO=[$0], ENAME=[$1], JOB=[$2], MGR=[$3], HIREDATE=[$4],"
            + " SAL=[$5], COMM=[$6], DEPTNO=[$7], arr=[array(1, 2, 3)],"
            + " result=[transform(array(1, 2, 3), (arr) -> *(arr, 10))])\n"
            + "      LogicalTableScan(table=[[scott, EMP]])\n";
    verifyLogical(root, expectedLogical);

    String expectedSparkSql =
        "SELECT TRANSFORM(ARRAY(1, 2, 3), `arr` -> `arr` * 10) `result`\n"
            + "FROM `scott`.`EMP`\n"
            + "LIMIT 1";
    verifyPPLToSparkSQL(root, expectedSparkSql);
  }

  @Test
  public void testMvmapWithAddition() {
    String ppl =
        "source=EMP | eval arr = array(1, 2, 3), result = mvmap(arr, arr + 5) | head 1 |"
            + " fields result";
    RelNode root = getRelNode(ppl);

    String expectedLogical =
        "LogicalProject(result=[$9])\n"
            + "  LogicalSort(fetch=[1])\n"
            + "    LogicalProject(EMPNO=[$0], ENAME=[$1], JOB=[$2], MGR=[$3], HIREDATE=[$4],"
            + " SAL=[$5], COMM=[$6], DEPTNO=[$7], arr=[array(1, 2, 3)],"
            + " result=[transform(array(1, 2, 3), (arr) -> +(arr, 5))])\n"
            + "      LogicalTableScan(table=[[scott, EMP]])\n";
    verifyLogical(root, expectedLogical);

    String expectedSparkSql =
        "SELECT TRANSFORM(ARRAY(1, 2, 3), `arr` -> `arr` + 5) `result`\n"
            + "FROM `scott`.`EMP`\n"
            + "LIMIT 1";
    verifyPPLToSparkSQL(root, expectedSparkSql);
  }

  @Test
  public void testMvmapWithNestedFunction() {
    // Test mvmap with mvindex as first argument - extracts field name from nested function
    // The lambda binds 'arr' and iterates over mvindex output
    String ppl =
        "source=EMP | eval arr = array(1, 2, 3, 4, 5), result = mvmap(mvindex(arr, 1, 3), arr * 10)"
            + " | head 1 | fields result";
    RelNode root = getRelNode(ppl);

    String expectedLogical =
        "LogicalProject(result=[$9])\n"
            + "  LogicalSort(fetch=[1])\n"
            + "    LogicalProject(EMPNO=[$0], ENAME=[$1], JOB=[$2], MGR=[$3], HIREDATE=[$4],"
            + " SAL=[$5], COMM=[$6], DEPTNO=[$7], arr=[array(1, 2, 3, 4, 5)],"
            + " result=[transform(ARRAY_SLICE(array(1, 2, 3, 4, 5), 1, +(-(3, 1), 1)), (arr) ->"
            + " *(arr, 10))])\n"
            + "      LogicalTableScan(table=[[scott, EMP]])\n";
    verifyLogical(root, expectedLogical);

    String expectedSparkSql =
        "SELECT TRANSFORM(ARRAY_SLICE(ARRAY(1, 2, 3, 4, 5), 1, 3 - 1 + 1), `arr` -> `arr`"
            + " * 10) `result`\n"
            + "FROM `scott`.`EMP`\n"
            + "LIMIT 1";
    verifyPPLToSparkSQL(root, expectedSparkSql);
  }

  @Test
  public void testArrayScalarEqualityUsesMembership() {
    String ppl =
        "source=EMP | eval tags=array('prod', 'blue') | where tags = 'prod' | head 1 |"
            + " fields tags";
    RelNode root = getRelNode(ppl);

    org.junit.Assert.assertTrue(
        org.apache.calcite.plan.RelOptUtil.toString(root).contains("ARRAY_CONTAINS"));
    verifyResult(root, "tags=[prod, blue]\n");
  }

  @Test
  public void testScalarArrayEqualityUsesMembership() {
    String ppl =
        "source=EMP | eval tags=array('prod', 'blue') | where 'blue' = tags | head 1 |"
            + " fields tags";
    RelNode root = getRelNode(ppl);

    org.junit.Assert.assertTrue(
        org.apache.calcite.plan.RelOptUtil.toString(root).contains("ARRAY_CONTAINS"));
    verifyResult(root, "tags=[prod, blue]\n");
  }

  @Test
  public void testArrayInUsesMembership() {
    String ppl =
        "source=EMP | eval tags=array('prod', 'blue') | where tags in ('missing', 'blue') |"
            + " head 1 | fields tags";
    RelNode root = getRelNode(ppl);

    String plan = org.apache.calcite.plan.RelOptUtil.toString(root);
    org.junit.Assert.assertTrue(plan.contains("ARRAY_CONTAINS"));
    org.junit.Assert.assertTrue(plan.contains("OR"));
    verifyResult(root, "tags=[prod, blue]\n");
  }

  @Test
  public void testArrayNotInUsesNegatedMembership() {
    String ppl =
        "source=EMP | eval tags=array('prod', 'blue') | where tags not in ('missing') |"
            + " head 1 | fields tags";
    RelNode root = getRelNode(ppl);

    String plan = org.apache.calcite.plan.RelOptUtil.toString(root);
    org.junit.Assert.assertTrue(plan.contains("ARRAY_CONTAINS"));
    org.junit.Assert.assertTrue(plan.contains("NOT"));
    verifyResult(root, "tags=[prod, blue]\n");
  }

  @Test
  public void testArrayEqualityInsideIfUsesMembership() {
    String ppl =
        "source=EMP | eval tags=array('prod', 'blue'), matched=if(tags = 'prod', 'yes', 'no') |"
            + " head 1 | fields matched";
    RelNode root = getRelNode(ppl);

    org.junit.Assert.assertTrue(
        org.apache.calcite.plan.RelOptUtil.toString(root).contains("ARRAY_CONTAINS"));
    verifyResult(root, "matched=yes\n");
  }

  @Test
  public void testArrayNotEqualUsesNoElementSemantics() {
    String ppl =
        "source=EMP | eval tags=array('prod', 'blue') | where tags != 'missing' | head 1 |"
            + " fields tags";
    RelNode root = getRelNode(ppl);

    String plan = org.apache.calcite.plan.RelOptUtil.toString(root);
    org.junit.Assert.assertTrue(plan.contains("ARRAY_CONTAINS"));
    org.junit.Assert.assertTrue(plan.contains("NOT"));
    verifyResult(root, "tags=[prod, blue]\n");
  }

  @Test
  public void testArrayGreaterThanUsesAnyElementSemantics() {
    String ppl =
        "source=EMP | eval tags=array('prod', 'blue') | where tags > 'orange' | head 1 |"
            + " fields tags";
    RelNode root = getRelNode(ppl);

    org.junit.Assert.assertTrue(
        org.apache.calcite.plan.RelOptUtil.toString(root).contains("array_any_compare"));
  }

  @Test
  public void testScalarLessThanArrayUsesAnyElementSemantics() {
    String ppl =
        "source=EMP | eval tags=array('prod', 'blue') | where 'orange' < tags | head 1 |"
            + " fields tags";
    RelNode root = getRelNode(ppl);

    org.junit.Assert.assertTrue(
        org.apache.calcite.plan.RelOptUtil.toString(root).contains("array_any_compare"));
  }

  @Test
  public void testArrayBetweenUsesAnyElementSemantics() {
    String ppl =
        "source=EMP | eval tags=array('prod', 'blue') | where tags between 'a' and 'c' |"
            + " head 1 | fields tags";
    RelNode root = getRelNode(ppl);

    org.junit.Assert.assertTrue(
        org.apache.calcite.plan.RelOptUtil.toString(root).contains("array_any_between"));
  }

  @Test
  public void testArrayLikeUsesAnyElementSemantics() {
    String ppl =
        "source=EMP | eval tags=array('prod', 'blue') | where like(tags, 'pro%') | head 1 |"
            + " fields tags";
    RelNode root = getRelNode(ppl);

    org.junit.Assert.assertTrue(
        org.apache.calcite.plan.RelOptUtil.toString(root).contains("array_any_compare"));
  }

  // ==================== Element-wise ARRAY expression semantics ====================

  @Test
  public void testUpperOnArrayReturnsStringArray() {
    String ppl =
        "source=EMP | eval tags=array('prod', 'blue'), up=upper(tags) | head 1 | fields up";
    RelNode root = getRelNode(ppl);

    org.junit.Assert.assertTrue(
        org.apache.calcite.plan.RelOptUtil.toString(root).contains("array_map_string"));
  }

  @Test
  public void testLowerOnArrayReturnsStringArray() {
    String ppl =
        "source=EMP | eval tags=array('PROD', 'BLUE'), lo=lower(tags) | head 1 | fields lo";
    RelNode root = getRelNode(ppl);

    org.junit.Assert.assertTrue(
        org.apache.calcite.plan.RelOptUtil.toString(root).contains("array_map_string"));
  }

  @Test
  public void testTrimVariantsOnArrayReturnStringArray() {
    for (String func : new String[] {"trim", "ltrim", "rtrim"}) {
      String ppl =
          "source=EMP | eval tags=array('  a  ', '  b  '), t="
              + func
              + "(tags) | head 1 | fields t";
      RelNode root = getRelNode(ppl);
      org.junit.Assert.assertTrue(
          "expected array_map_string for " + func,
          org.apache.calcite.plan.RelOptUtil.toString(root).contains("array_map_string"));
    }
  }

  @Test
  public void testSubstringOnArrayReturnsStringArray() {
    String ppl =
        "source=EMP | eval tags=array('prod', 'blue'), s=substring(tags, 1, 2) | head 1 | fields s";
    RelNode root = getRelNode(ppl);

    org.junit.Assert.assertTrue(
        org.apache.calcite.plan.RelOptUtil.toString(root).contains("array_map_string"));
  }

  @Test
  public void testReplaceOnArrayReturnsStringArray() {
    String ppl =
        "source=EMP | eval tags=array('prod', 'blue'), r=replace(tags, 'o', '0') | head 1 |"
            + " fields r";
    RelNode root = getRelNode(ppl);

    org.junit.Assert.assertTrue(
        org.apache.calcite.plan.RelOptUtil.toString(root).contains("array_map_string"));
  }

  @Test
  public void testReverseOnArrayReturnsStringArray() {
    String ppl =
        "source=EMP | eval tags=array('prod', 'blue'), rv=reverse(tags) | head 1 | fields rv";
    RelNode root = getRelNode(ppl);

    org.junit.Assert.assertTrue(
        org.apache.calcite.plan.RelOptUtil.toString(root).contains("array_map_string"));
  }

  @Test
  public void testLengthOnArrayReturnsIntegerArray() {
    String ppl = "source=EMP | eval tags=array('prod', 'blue'), l=length(tags) | head 1 | fields l";
    RelNode root = getRelNode(ppl);

    org.junit.Assert.assertTrue(
        org.apache.calcite.plan.RelOptUtil.toString(root).contains("array_map_integer"));
  }

  @Test
  public void testAsciiOnArrayReturnsIntegerArray() {
    String ppl = "source=EMP | eval tags=array('prod', 'blue'), a=ascii(tags) | head 1 | fields a";
    RelNode root = getRelNode(ppl);

    org.junit.Assert.assertTrue(
        org.apache.calcite.plan.RelOptUtil.toString(root).contains("array_map_integer"));
  }

  @Test
  public void testPositionAndLocateOnArrayReturnIntegerArrays() {
    for (String expression : new String[] {"position('o' IN tags)", "locate('o', tags)"}) {
      String ppl =
          "source=EMP | eval tags=array('prod', 'blue'), p=" + expression + " | head 1 | fields p";
      RelNode root = getRelNode(ppl);
      org.junit.Assert.assertTrue(
          "expected array_map_integer for " + expression,
          org.apache.calcite.plan.RelOptUtil.toString(root).contains("array_map_integer"));
    }
  }

  @Test
  public void testNullifOnArrayReplacesMatchingElements() {
    String ppl =
        "source=EMP | eval tags=array('prod', 'blue'), n=nullif(tags, 'prod') | head 1 | fields n";
    RelNode root = getRelNode(ppl);

    org.junit.Assert.assertTrue(
        org.apache.calcite.plan.RelOptUtil.toString(root).contains("array_nullif"));
  }

  @Test
  public void testCoalesceOnArrayUsesArrayCoalesce() {
    String ppl =
        "source=EMP | eval tags=array('prod', 'blue'), c=coalesce(tags, 'fallback') | head 1 |"
            + " fields c";
    RelNode root = getRelNode(ppl);

    org.junit.Assert.assertTrue(
        org.apache.calcite.plan.RelOptUtil.toString(root).contains("array_coalesce"));
  }

  @Test
  public void testScalarStringFunctionUnaffectedByElementWiseRewrite() {
    String ppl = "source=EMP | eval up=upper(ENAME) | head 1 | fields up";
    RelNode root = getRelNode(ppl);

    String plan = org.apache.calcite.plan.RelOptUtil.toString(root);
    org.junit.Assert.assertFalse(plan.contains("array_map_string"));
    org.junit.Assert.assertFalse(plan.contains("array_map_integer"));
  }
}
