package lk.rush.homeservicego.models;

// Serializable allows passing Service object between activities via Intent
public class Service implements java.io.Serializable {

    private String serviceId;
    private String name;
    private String category;
    private String description;
    private double price;
    private String imageUrl;
    private String providerPhone;
    private double latitude;   // GPS latitude for Maps
    private double longitude;  // GPS longitude for Maps

    public Service() {
    }

    public Service(String serviceId, String name, String category,
                   String description, double price,
                   String imageUrl, String providerPhone,
                   double latitude, double longitude) {
        this.serviceId     = serviceId;
        this.name          = name;
        this.category      = category;
        this.description   = description;
        this.price         = price;
        this.imageUrl      = imageUrl;
        this.providerPhone = providerPhone;
        this.latitude      = latitude;
        this.longitude     = longitude;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getProviderPhone() {
        return providerPhone;
    }

    public void setProviderPhone(String providerPhone) {
        this.providerPhone = providerPhone;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}