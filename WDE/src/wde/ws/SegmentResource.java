package wde.ws;

import wde.data.GeoCode;
import wde.data.GeoCodeList;
import wde.data.Segment;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

@Path("segment")
public class SegmentResource {

    @Context
    HttpServletRequest request;
    @Context
    HttpServletResponse response;
    @Context
    ServletContext context;

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<Segment> getSegments() {

        List<Segment> segments = new ArrayList<Segment>();

		/* SEGMENT 1 */
        Segment segment = new Segment();
        segment.setId(1);
        segment.setName("190");

        List<GeoCode> geoCodes = new ArrayList<GeoCode>();
        GeoCode geoCode = new GeoCode();
        geoCode.setLatitude(43.88541347);
        geoCode.setLongitude(-92.48786883);
        geoCodes.add(geoCode);

        geoCode = new GeoCode();
        geoCode.setLatitude(43.8853725);
        geoCode.setLongitude(-92.48800172);
        geoCodes.add(geoCode);

        geoCode = new GeoCode();
        geoCode.setLatitude(43.88527395);
        geoCode.setLongitude(-92.48831265);
        geoCodes.add(geoCode);

        geoCode = new GeoCode();
        geoCode.setLatitude(43.88501313);
        geoCode.setLongitude(-92.48904787);
        geoCodes.add(geoCode);

        geoCode = new GeoCode();
        geoCode.setLatitude(43.88442797);
        geoCode.setLongitude(-92.49108279);
        geoCodes.add(geoCode);

        geoCode = new GeoCode();
        geoCode.setLatitude(43.88392146);
        geoCode.setLongitude(-92.49258792);
        geoCodes.add(geoCode);

        geoCode = new GeoCode();
        geoCode.setLatitude(43.88386824);
        geoCode.setLongitude(-92.49276811);
        geoCodes.add(geoCode);

        geoCode = new GeoCode();
        geoCode.setLatitude(43.881506);
        geoCode.setLongitude(-92.50020022);
        geoCodes.add(geoCode);

        geoCode = new GeoCode();
        geoCode.setLatitude(43.88065245);
        geoCode.setLongitude(-92.50267901);
        geoCodes.add(geoCode);

        geoCode = new GeoCode();
        geoCode.setLatitude(43.87983584);
        geoCode.setLongitude(-92.5043406);
        geoCodes.add(geoCode);

        geoCode = new GeoCode();
        geoCode.setLatitude(43.87944677);
        geoCode.setLongitude(-92.50493975);
        geoCodes.add(geoCode);

        geoCode = new GeoCode();
        geoCode.setLatitude(43.87846197);
        geoCode.setLongitude(-92.50623232);
        geoCodes.add(geoCode);

        geoCode = new GeoCode();
        geoCode.setLatitude(43.87757968);
        geoCode.setLongitude(-92.50729297);
        geoCodes.add(geoCode);

        geoCode = new GeoCode();
        geoCode.setLatitude(43.8755181);
        geoCode.setLongitude(-92.50944615);
        geoCodes.add(geoCode);

        geoCode = new GeoCode();
        geoCode.setLatitude(43.87496709);
        geoCode.setLongitude(-92.51008157);
        geoCodes.add(geoCode);

        geoCode = new GeoCode();
        geoCode.setLatitude(43.8737005);
        geoCode.setLongitude(-92.51137917);
        geoCodes.add(geoCode);

        geoCode = new GeoCode();
        geoCode.setLatitude(43.86740707);
        geoCode.setLongitude(-92.51821963);
        geoCodes.add(geoCode);

        GeoCodeList geoCodeList = new GeoCodeList(geoCodes);
        segment.setGeoCodes(geoCodeList);
        segments.add(segment);

		/* SEGMENT 2 */
        segment = new Segment();
        segment.setId(2);
        segment.setName("666");

        geoCodes = new ArrayList<GeoCode>();

        geoCode = new GeoCode();
        geoCode.setLatitude(43.86740707);
        geoCode.setLongitude(-92.51821963);
        geoCodes.add(geoCode);

        geoCode = new GeoCode();
        geoCode.setLatitude(43.86282214);
        geoCode.setLongitude(-92.52310805);
        geoCodes.add(geoCode);

        geoCode = new GeoCode();
        geoCode.setLatitude(43.8627725);
        geoCode.setLongitude(-92.52316098);
        geoCodes.add(geoCode);

        geoCode = new GeoCode();
        geoCode.setLatitude(43.86254431);
        geoCode.setLongitude(-92.52340521);
        geoCodes.add(geoCode);

        geoCode = new GeoCode();
        geoCode.setLatitude(43.85989729);
        geoCode.setLongitude(-92.52626312);
        geoCodes.add(geoCode);

        geoCode = new GeoCode();
        geoCode.setLatitude(43.85855917);
        geoCode.setLongitude(-92.52755711);
        geoCodes.add(geoCode);

        geoCode = new GeoCode();
        geoCode.setLatitude(43.85596278);
        geoCode.setLongitude(-92.53022102);
        geoCodes.add(geoCode);

        geoCode = new GeoCode();
        geoCode.setLatitude(43.85299776);
        geoCode.setLongitude(-92.5332166);
        geoCodes.add(geoCode);

        geoCode = new GeoCode();
        geoCode.setLatitude(43.85010043);
        geoCode.setLongitude(-92.53608822);
        geoCodes.add(geoCode);

        geoCode = new GeoCode();
        geoCode.setLatitude(43.84976235);
        geoCode.setLongitude(-92.53643513);
        geoCodes.add(geoCode);

        geoCode = new GeoCode();
        geoCode.setLatitude(43.84419879);
        geoCode.setLongitude(-92.54198505);
        geoCodes.add(geoCode);

        geoCodeList = new GeoCodeList(geoCodes);
        segment.setGeoCodes(geoCodeList);
        segments.add(segment);

		/* SEGMENT 8 */
        segment = new Segment();
        segment.setId(8);
        segment.setName("I90 7");

        geoCodes = new ArrayList<GeoCode>();

        geoCode = new GeoCode();
        geoCode.setLatitude(43.80494651);
        geoCode.setLongitude(-92.58826565);
        geoCodes.add(geoCode);

        geoCode = new GeoCode();
        geoCode.setLatitude(43.80645354);
        geoCode.setLongitude(-92.58653389);
        geoCodes.add(geoCode);

        geoCode = new GeoCode();
        geoCode.setLatitude(43.81560127);
        geoCode.setLongitude(-92.57706111);
        geoCodes.add(geoCode);

        geoCode = new GeoCode();
        geoCode.setLatitude(43.8181213);
        geoCode.setLongitude(-92.57433757);
        geoCodes.add(geoCode);

        geoCode = new GeoCode();
        geoCode.setLatitude(43.82734977);
        geoCode.setLongitude(-92.56192702);
        geoCodes.add(geoCode);

        geoCodeList = new GeoCodeList(geoCodes);
        segment.setGeoCodes(geoCodeList);
        segments.add(segment);

        return segments;
    }
}
