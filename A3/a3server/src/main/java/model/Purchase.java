package model;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Objects;

public class Purchase {
  @SerializedName("items")
  private List<PurchaseItems> items = null;

  public List<PurchaseItems> getItems() {
    return items;
  }

  public void setItems(List<PurchaseItems> items) {
    this.items = items;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Purchase purchase = (Purchase) o;
    return Objects.equals(this.items, purchase.items);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(" {\n");

    sb.append("    \"items\": ").append(toIndentedString(items)).append("\n");
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
