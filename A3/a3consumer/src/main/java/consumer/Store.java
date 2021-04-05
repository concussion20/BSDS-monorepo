package consumer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

class Store {
  // to find "What are the top 10 most purchased items at Store N"
  static ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> purchasesInStores =
      new ConcurrentHashMap<>();
  // to find "Which are the top 10 stores for sales for item N"
  static ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> itemsSale =
      new ConcurrentHashMap<>();

  public static List<Entry<String, Integer>> getTop10ItemsInStore(String storeId) {
    ConcurrentHashMap<String, Integer> storeMap = purchasesInStores.get(storeId);
    if (storeMap == null) {
      return null;
    }

    Iterator<Entry<String, Integer>> itr = storeMap.entrySet().iterator();
    PriorityQueue<Entry<String, Integer>> pq = new PriorityQueue<>(
        Comparator.comparingInt(Entry::getValue));

    while(itr.hasNext())
    {
      Map.Entry<String, Integer> entry = itr.next();
      try {
        pq.offer(entry);
      } catch (Exception e) {
        e.printStackTrace();
      }
      if (pq.size() > 10) {
        pq.poll();
      }
    }

    List<Entry<String, Integer>> list = new ArrayList<>(pq);
    Collections.reverse(list);
    return list;
  }

  public static List<Entry<String, Integer>> getTop5StoresForItem(String itemId) {
    ConcurrentHashMap<String, Integer> itemMap = itemsSale.get(itemId);
    if (itemMap == null) {
      return null;
    }

    Iterator<Entry<String, Integer>> itr = itemMap.entrySet().iterator();
    PriorityQueue<Entry<String, Integer>> pq = new PriorityQueue<>(
        Comparator.comparingInt(Entry::getValue));

    while(itr.hasNext())
    {
      Map.Entry<String, Integer> entry = itr.next();
      try {
        pq.offer(entry);
      } catch (Exception e) {
        e.printStackTrace();
      }
      if (pq.size() > 10) {
        pq.poll();
      }
    }

    List<Entry<String, Integer>> list = new ArrayList<>(pq);
    Collections.reverse(list);
    return list;
  }

}
