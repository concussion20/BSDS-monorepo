package dynamo;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import java.util.HashMap;
import java.util.Map;

public class Purchaser {

  public static boolean addPurchase(AmazonDynamoDB client, String tableName, String uuid, HashMap<String, String> valuesMap) {
    HashMap<String, AttributeValue> itemValues = new HashMap<>();

    itemValues.put("ID", new AttributeValue(uuid));

    for (Map.Entry<String, String> entry : valuesMap.entrySet()) {
      itemValues.put(entry.getKey(), new AttributeValue(entry.getValue()));
    }

    try {
      client.putItem(tableName, itemValues);
    } catch (ResourceNotFoundException e) {
      System.err.format("Error: The table \"%s\" can't be found.\n", tableName);
      System.err.println("Be sure that it exists and that you've typed its name correctly!");
      return false;
    } catch (AmazonServiceException e) {
      System.err.println(e.getMessage());
      return false;
    }

    return true;
  }
}
