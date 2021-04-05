package model;

import com.google.gson.annotations.SerializedName;
import java.util.Objects;

public class PurchaseItems {

  @SerializedName("ItemID")
  private String itemID = null;

  @SerializedName("numberOfItems:")
  private Integer numberOfItems = null;

  /**
   * Get itemID
   * @return itemID
   **/
  public String getItemID() {
    return itemID;
  }

  public void setItemID(String itemID) {
    this.itemID = itemID;
  }

  public Integer getNumberOfItems() {
    return numberOfItems;
  }

  public void setNumberOfItems(Integer numberOfItems) {
    this.numberOfItems = numberOfItems;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PurchaseItems purchaseItems = (PurchaseItems) o;
    return Objects.equals(this.itemID, purchaseItems.itemID) &&
        Objects.equals(this.numberOfItems, purchaseItems.numberOfItems);
  }

  @Override
  public int hashCode() {
    return Objects.hash(itemID, numberOfItems);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(" {\n");

    sb.append("    \"itemID\": \"").append(toIndentedString(itemID)).append("\",\n");
    sb.append("    \"numberOfItems\": ").append(toIndentedString(numberOfItems)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
