/*
 * MIT License
 *
 * Copyright (c) 2024 FHIRest community
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package ee.fhir.fhirest.util.sql;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class SqlBuilder implements Serializable {
  protected StringBuilder sb = new StringBuilder();
  protected List<Object> params = new ArrayList<>();

  public SqlBuilder() {
    //
  }

  public SqlBuilder(String sql, Object... params) {
    append(sql, params);
  }

  public SqlBuilder add(Object... params) {
    if (params != null) {
      this.params.addAll(Arrays.asList(params));
    }
    return this;
  }

  public SqlBuilder add(Collection<?> params) {
    if (params != null) {
      this.params.addAll(params);
    }
    return this;
  }

  public SqlBuilder append(String sql, Object... params) {
    if (!isEmpty() && !sql.startsWith(" ") && !sql.startsWith(")")) {
      sb.append(" ");
    }
    sb.append(sql);
    return add(params);
  }

  public SqlBuilder append(SqlBuilder sql) {
    return sql == null ? this : append(sql.getSql(), sql.getParams());
  }

  public SqlBuilder appendIfTrue(boolean condition, String sql, Object... params) {
    return condition ? append(sql, params) : this;
  }

  public SqlBuilder appendIfNotNull(String str, Object param) {
    return param == null ? this : append(" ").append(str, param).append(" ");
  }

  public SqlBuilder and() {
    return append("AND");
  }

  public SqlBuilder and(SqlBuilder sql) {
    return and().append(sql);
  }

  public SqlBuilder and(String str, Object... params) {
    return and().append(str, params);
  }

  public SqlBuilder or() {
    return append("OR");
  }

  public SqlBuilder or(SqlBuilder sql) {
    return or().append(sql);
  }

  public SqlBuilder or(String str, Object... params) {
    return or().append(str, params);
  }

  public SqlBuilder eq(String str, Object param) {
    append(str);
    if (param == null) {
      return append("IS NULL");
    }
    return append("= ?", param);
  }

  public SqlBuilder append(Collection<SqlBuilder> sqls, String separator) {
    String delim = "";
    appendIfTrue(sqls.size() > 1, "(");
    for (SqlBuilder sb : sqls) {
      append(delim).append(sb);
      delim = separator;
    }
    appendIfTrue(sqls.size() > 1, ")");
    return this;
  }

  public SqlBuilder in(String columnName, Object... params) {
    return in(columnName, Arrays.asList(params));
  }

  public SqlBuilder in(String columnName, Collection<?> params) {
    if (CollectionUtils.isEmpty(params)) {
      return this;
    }
    if (params.size() == 1) {
      return this.append(columnName + " = ?", params.iterator().next());
    }
    String sql = columnName + " IN(?" + StringUtils.repeat(",?", params.size() - 1) + ")";
    return append(sql, params.toArray());
  }

  public Object[] getParams() {
    return params.toArray();
  }

  public List<Object> getParamsAsList() {
    return params;
  }

  public StringBuilder getStringBuilder() {
    return sb;
  }

  public boolean isEmpty() {
    return sb.isEmpty();
  }

  public String getSql() {
    return sb.toString();
  }

  @Override
  public String toString() {
    return "SQL: [\n  " + sb.toString() + "\n], args: {\n  " + params + "\n}, pretty: {\n  " + getPretty() + "\n}";
  }

  public String getPretty() {
    int i = 0;
    Object[] p = new Object[params.size()];
    for (Object param : params) {
      if (param == null) {
        p[i++] = "NULL";
      } else {
        p[i++] = param instanceof String ? ("'" + param + "'") : param.toString();
      }
    }
    return String.format(sb.toString().replaceAll("%", "%%").replaceAll("\\?", "%s"), p);
  }

}
