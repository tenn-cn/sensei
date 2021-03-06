/**
 * This software is licensed to you under the Apache License, Version 2.0 (the
 * "Apache License").
 *
 * LinkedIn's contributions are made under the Apache License. If you contribute
 * to the Software, the contributions will be deemed to have been made under the
 * Apache License, unless you expressly indicate otherwise. Please do not make any
 * contributions that would be inconsistent with the Apache License.
 *
 * You may obtain a copy of the Apache License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, this software
 * distributed under the Apache License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Apache
 * License for the specific language governing permissions and limitations for the
 * software governed under the Apache License.
 *
 * © 2012 LinkedIn Corp. All Rights Reserved.  
 */
package com.senseidb.search.req;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.search.Explanation;

import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.BrowseHit;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.FacetAccessible;


public class SenseiResult extends BrowseResult implements AbstractSenseiResult
{

  private static final long serialVersionUID = 1L;

  private String _parsedQuery = null;

  private List<SenseiError> errors;
 
  public SenseiHit[] getSenseiHits()
  {
    BrowseHit[] hits = getHits();
    if (hits == null || hits.length == 0)
    {
      return new SenseiHit[0];
    }
    return (SenseiHit[]) hits;
  }

  public void setParsedQuery(String query)
  {
    _parsedQuery = query;
  }

  public String getParsedQuery()
  {
    return _parsedQuery;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof SenseiResult)) return false;
    SenseiResult b = (SenseiResult)o;

    if(getParsedQuery() == null)
      return b.getParsedQuery() == null;

    if (!getParsedQuery().equals(b.getParsedQuery())) return false;

    // TODO: move this into BrowseResult equals
    if (!senseiHitsAreEqual(getSenseiHits(), b.getSenseiHits())) return false;
    if (getTid() != b.getTid()) return false;
    if (getTime() != b.getTime()) return false;
    if (getNumHits() != getNumHits()) return false;
    if (getNumGroups() != getNumGroups()) return false;
    if (getTotalDocs() != getTotalDocs()) return false;
    if (!facetMapsAreEqual(getFacetMap(), b.getFacetMap())) return false;

    return true;
  }

  private boolean senseiHitsAreEqual(SenseiHit[] a, SenseiHit[] b) {
    if (a == null)
      return b == null;
    if (a.length != b.length)
      return false;

    for (int i = 0; i < a.length; i++) {
      if (a[i].getUID() != b[i].getUID())
        return false;
      if (a[i].getDocid() != b[i].getDocid())
        return false;
      if (a[i].getScore() != b[i].getScore())
        return false;
      if (a[i].getGroupValue() == null || b[i].getGroupValue() == null) {
        if(!(a[i].getGroupValue() == null && b[i].getGroupValue() == null))
          return false;
      } else {
        if (!a[i].getGroupValue().equals(b[i].getGroupValue()))
          return false;
      }
      if (a[i].getRawGroupValue() == null || b[i].getRawGroupValue() == null) {
        if(!(a[i].getRawGroupValue() == null && b[i].getRawGroupValue() == null))
          return false;
      } else {
        if(a[i].getRawGroupValue() instanceof long[]) {
          if(!(b[i].getRawGroupValue() instanceof long[] &&
              Arrays.equals((long[]) a[i].getRawGroupValue(), (long[]) b[i].getRawGroupValue())))
            return false;
        } else if(!a[i].getRawGroupValue().equals(b[i].getRawGroupValue())) {
          return false;
        }
      }

      if (a[i].getGroupHitsCount() != b[i].getGroupHitsCount())
        return false;
      if (!senseiHitsAreEqual(a[i].getSenseiGroupHits(), b[i].getSenseiGroupHits()))
        return false;
      if (!explanationsAreEqual(a[i].getExplanation(), b[i].getExplanation()))
        return false;

      if (!storedFieldsAreEqual(a[i].getStoredFields(), b[i].getStoredFields()))
        return false;

      if (!fieldValuesAreEqual(a[i].getFieldValues(), b[i].getFieldValues()))
        return false;
      if (!rawFieldValuesAreEqual(a[i].getRawFieldValues(), b[i].getRawFieldValues()))
        return false;
    }

    return true;
  }

  private boolean fieldValuesAreEqual(Map<String, String[]> fieldValuesA, Map<String, String[]> fieldValuesB) {
    if(fieldValuesA == null || fieldValuesB == null) {
      return fieldValuesA == null && fieldValuesB == null;
    }

    if(fieldValuesA.size() != fieldValuesB.size())
      return false;

    for(Entry<String, String[]> entryA : fieldValuesA.entrySet()) {
      String keyA = entryA.getKey();
      String[] valueA = entryA.getValue();
      String[] valueB = fieldValuesB.get(keyA);

      if(valueB == null) {
        return false;
      }
      return Arrays.equals(valueA, valueB);
    }

    return true;
  }

  private boolean rawFieldValuesAreEqual(Map<String, Object[]> fieldValuesA, Map<String, Object[]> fieldValuesB) {
    if(fieldValuesA == null || fieldValuesB == null) {
      return fieldValuesA == null && fieldValuesB == null;
    }

    if(fieldValuesA.size() != fieldValuesB.size()) return false;

    // TODO: Should we enforce iteration order?
    for(Entry<String, Object[]> entryA : fieldValuesA.entrySet()) {
      String keyA = entryA.getKey();
      Object[] valueA = entryA.getValue();
      Object[] valueB = fieldValuesB.get(keyA);

      if(valueB == null) {
        return false;
      }
      return Arrays.equals(valueA, valueB);
    }

    return true;
  }


  private boolean storedFieldsAreEqual(Document a, Document b) {
    if (a == null) return b == null;
    else if(b == null)
      return false;

    List<Fieldable> aFields = a.getFields();
    List<Fieldable> bFields = b.getFields();

    int aFieldSize = aFields == null ? 0 : aFields.size();
    int bFieldSize = bFields == null ? 0 : bFields.size();

    if(aFieldSize != bFieldSize) {
      return false;
    }

    for(int i = 0; i < aFieldSize; i++) {
      Fieldable aField = aFields.get(i);
      Fieldable bField = bFields.get(i);

      if(aField.getBoost() != bField.getBoost())
        return false;

      if(aField.isBinary() != bField.isBinary())
        return false;

      if(aField.isIndexed() != bField.isIndexed())
        return false;

      if(aField.isLazy() != bField.isLazy())
        return false;

      if(aField.isStored() != bField.isStored())
        return false;

      if(aField.getOmitNorms() != bField.getOmitNorms())
        return false;

      if(aField.getBinaryLength() != bField.getBinaryLength())
        return false;

      if(aField.getBinaryOffset() != bField.getBinaryOffset())
        return false;

      if(!Arrays.equals(aField.getBinaryValue(), bField.getBinaryValue()))
        return false;
    }

    return true;
  }

  private boolean explanationsAreEqual(Explanation a, Explanation b) {
    // TODO: is comparing the document strings adequate?
    if(a == null) {
      return b == null;
    } else {
      return StringUtils.equals(a.getDescription(), b.getDescription());
    }
  }

  private boolean facetMapsAreEqual(Map<String, FacetAccessible> a, Map<String, FacetAccessible> b) {
    if (a == null) return b == null;
    if (a.size() != b.size()) return false;

    for (Entry<String,FacetAccessible> entry : a.entrySet()) {
      String fieldName = entry.getKey();
      if (!b.containsKey(fieldName)) return false;
      if (!facetAccessibleAreEqual(entry.getValue(), b.get(fieldName))) return false;
    }

    return true;
  }

  private boolean facetAccessibleAreEqual(FacetAccessible a, FacetAccessible b) {
    if (a == null) return b == null;
    if (a.getFacets().size() != b.getFacets().size()) return false;

    List<BrowseFacet> al = a.getFacets();
    List<BrowseFacet> bl = b.getFacets();

    if (!Arrays.equals(al.toArray(new BrowseFacet[al.size()]), bl.toArray(new BrowseFacet[bl.size()]))) return false;

    return true;
  }

  public List<SenseiError> getErrors() {
    if (errors == null)
      errors = new ArrayList<SenseiError>();

    return errors ;
  }

  public void addError(SenseiError error) {
    if (errors == null)
      errors = new ArrayList<SenseiError>();

    errors.add(error);
  }

  
   
}
