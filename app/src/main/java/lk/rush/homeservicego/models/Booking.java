package lk.rush.homeservicego.models;

public class Booking {
    private String bookingId;
    private String userId;
    private String userName;    // stored for admin display
    private String serviceId;
    private String serviceName; // stored so we don't need extra Firestore lookup
    private String bookingDate;
    private String bookingTime;
    private String status; // pending, confirmed, completed

    // Location fields — where the user wants the service done
    private String locationName; // human-readable address (e.g. "123 Main St, Colombo")
    private double locationLat;  // GPS latitude
    private double locationLng;  // GPS longitude

    private long createdAt;

    // No-arg constructor required for Firestore
    public Booking() {}

    public Booking(String bookingId, String userId, String userName,
                   String serviceId, String serviceName,
                   String bookingDate, String bookingTime, String status,
                   String locationName, double locationLat, double locationLng) {
        this.bookingId    = bookingId;
        this.userId       = userId;
        this.userName     = userName;
        this.serviceId    = serviceId;
        this.serviceName  = serviceName;
        this.bookingDate  = bookingDate;
        this.bookingTime  = bookingTime;
        this.status       = status;
        this.locationName = locationName;
        this.locationLat  = locationLat;
        this.locationLng  = locationLng;
    }

    public Booking(String bookingId, String userId, String userName,
                   String serviceId, String serviceName,
                   String bookingDate, String bookingTime, String status,
                   String locationName, double locationLat, double locationLng,
                   long createdAt) {
        this(bookingId, userId, userName, serviceId, serviceName, bookingDate, bookingTime, status, locationName, locationLat, locationLng);
        this.createdAt    = createdAt;
    }

    public String getBookingId()              { return bookingId; }
    public void   setBookingId(String v)      { bookingId = v; }

    public String getUserId()                 { return userId; }
    public void   setUserId(String v)         { userId = v; }

    public String getUserName()               { return userName; }
    public void   setUserName(String v)       { userName = v; }

    public String getServiceId()              { return serviceId; }
    public void   setServiceId(String v)      { serviceId = v; }

    public String getServiceName()            { return serviceName; }
    public void   setServiceName(String v)    { serviceName = v; }

    public String getBookingDate()            { return bookingDate; }
    public void   setBookingDate(String v)    { bookingDate = v; }

    public String getBookingTime()            { return bookingTime; }
    public void   setBookingTime(String v)    { bookingTime = v; }

    public String getStatus()                 { return status; }
    public void   setStatus(String v)         { status = v; }

    public String getLocationName()           { return locationName; }
    public void   setLocationName(String v)   { locationName = v; }

    public double getLocationLat()            { return locationLat; }
    public void   setLocationLat(double v)    { locationLat = v; }

    public double getLocationLng()            { return locationLng; }
    public void   setLocationLng(double v)    { locationLng = v; }

    public long   getCreatedAt()              { return createdAt; }
    public void   setCreatedAt(long v)        { createdAt = v; }
}
