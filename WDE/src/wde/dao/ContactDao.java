/************************************************************************
 * Source filename: ContactDao.java
 * <p/>
 * Creation date: Feb 22, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WxDE
 * <p/>
 * Objective: Data Access Object for the contact table
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.dao;

import org.apache.log4j.Logger;
import wde.metadata.Contact;
import wde.metadata.TimeVariantMetadata;
import wde.util.QueryString;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class ContactDao {

    private static final Logger logger = Logger.getLogger(ContactDao.class);

    private static final String SELECT_STATICID_QUERY = "SELECT * FROM meta.contact WHERE staticId = ? ORDER BY id desc limit 1";

    private static ContactDao instance;

    private DatabaseManager db = null;

    private String connId = null;

    private String sharedConnId = null;

    private MetadataDao md;

    private HashMap<String, TimeVariantMetadata> contactMap = null;

    /**
     * Constructor
     */
    private ContactDao() {
        db = DatabaseManager.getInstance();
        connId = db.getConnection();
        sharedConnId = MetadataDao.getInstance().getConnId();
        md = MetadataDao.getInstance();

        contactMap = new HashMap<>();
        updateContactMap();
    }

    /**
     * @return ContactDao
     */
    public static ContactDao getInstance() {
        if (instance == null)
            instance = new ContactDao();

        return instance;
    }

    /**
     * @return the contactMap
     */
    public HashMap<String, TimeVariantMetadata> getContactMap() {
        return contactMap;
    }

    /**
     * @param staticId
     * @return the most recent record that has the same staticId
     */
    public Contact getContact(String staticId) {
        Contact contact = null;

        PreparedStatement ps = db.prepareStatement(connId, SELECT_STATICID_QUERY);
        ResultSet rs = null;

        try {
            ps.setInt(1, Integer.valueOf(staticId));
            rs = ps.executeQuery();

            if (rs != null && rs.next()) {
                contact = new Contact();
                contact.setId(rs.getInt("id"));
                contact.setStaticId(staticId);
                contact.setUpdateTime(rs.getTimestamp("updateTime"));
                contact.setToTime(rs.getTimestamp("toTime"));
                contact.setName(rs.getString("name"));
                contact.setTitle(rs.getString("title"));
                contact.setOrgId(rs.getInt("orgId"));
                contact.setPhonePrimary(rs.getString("phonePrimary"));
                contact.setPhoneAlt(rs.getString("phoneAlt"));
                contact.setPhoneMobile(rs.getString("phoneMobile"));
                contact.setFax(rs.getString("fax"));
                contact.setEmail(rs.getString("email"));
                contact.setAddress1(rs.getString("address1"));
                contact.setAddress2(rs.getString("address2"));
                contact.setCity(rs.getString("city"));
                contact.setState(rs.getString("state"));
                contact.setZip(rs.getString("zip"));
                contact.setCountry(rs.getString("country"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        } finally {
            try {
                rs.close();
                rs = null;
                ps.close();
                ps = null;
            } catch (SQLException se) {
                // ignore
            }
        }

        return contact;
    }

    /**
     * Updates the contact map based on what's currently in the database
     */
    public void updateContactMap() {
        String sql = "SELECT DISTINCT(staticId) FROM meta.contact";
        ResultSet rs = db.query(connId, sql);
        try {
            ArrayList<String> staticIds = new ArrayList<String>();
            while (rs != null && rs.next()) {
                staticIds.add(String.valueOf(rs.getInt("staticId")));
            }

            for (String staticId : staticIds) {
                Contact contact = getContact(staticId);
                contactMap.put(contact.getStaticId(), contact);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        } finally {
            try {
                rs.close();
                rs = null;
            } catch (SQLException se) {
                // ignore
            }
        }
    }

    /**
     * @param contact
     * @param atomic - false if this is part of a batch processing
     * @return true if a change record in organization needs to be inserted; false if no change is found
     */
    public boolean updateContact(Contact contact, boolean atomic) {
        String key = contact.getStaticId();

        Contact dbContact = (Contact) contactMap.get(key);

        if (dbContact == null || !dbContact.equals(contact)) {

            if (dbContact == null)
                logger.info("inserting contact " + key);
            else
                logger.info("updating contact " + key);

            Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());
            String sql = "INSERT INTO meta.contact (staticId, updateTime, name, title, orgId, phonePrimary, phoneAlt, phoneMobile, "
                    + "fax, email, address1, address2, city, state, zip, country)  VALUES ("
                    + key + ","
                    + "'" + now + "',"
                    + QueryString.convert(contact.getName(), false)
                    + QueryString.convert(contact.getTitle(), false)
                    + QueryString.convertId(contact.getOrgId(), false)
                    + QueryString.convert(contact.getPhonePrimary(), false)
                    + QueryString.convert(contact.getPhoneAlt(), false)
                    + QueryString.convert(contact.getPhoneMobile(), false)
                    + QueryString.convert(contact.getFax(), false)
                    + QueryString.convert(contact.getEmail(), false)
                    + QueryString.convert(contact.getAddress1(), false)
                    + QueryString.convert(contact.getAddress2(), false)
                    + QueryString.convert(contact.getCity(), false)
                    + QueryString.convert(contact.getState(), false)
                    + QueryString.convert(contact.getZip(), false)
                    + QueryString.convert(contact.getCountry(), true)
                    + ");";

            if (atomic) {
                db.update(connId, sql);

                // Update the id field
                contact = getContact(key);
                contactMap.put(key, contact);
            } else
                db.update(sharedConnId, sql);

            if (dbContact != null)
                md.inactivateRecord("meta.contact", dbContact.getId(), now);

            if (atomic)
                md.lastUpate("contact", now);

            return true;
        }

        return false;
    }
}
