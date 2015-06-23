<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!-- box, radius, info functions and center map input box -->
    <div class="navbar fhwa-gradient-color ie-fix-navbar" id="bottom-nav" style="position: absolute; bottom:0;width: 100%; margin-bottom:0; border-radius:0;z-index:4;">
        <a class="btn btn-info navbar-btn pull-left tip" id="boundbox_btn" href="#tab1">Bounding Box
            <span>Use a box for mapping..</span></a>
        <a class="btn btn-info navbar-btn pull-left tip" id="radius_btn" href="#tab2" style="margin-left:5px;">Radius
            <span>Use a circle for mapping..</span></a>
        <button class="btn btn-inverse navbar-btn pull-left tip" id="info_button" style="margin-left:5px;">Info
            <span>Click to toggle Coordinates</span></button>
        <div class="clear-fix"></div>
        <p class="navbar-text align-center" id="coordinates_rectangle" style="display:none; color:#ccc;">
            <strong>NorthEast Latitude:</strong> <span id="neLat"></span>  
            <strong>NorthEast Longitude:</strong> <span id="neLng"></span> 
            <strong>SouthWest Latitude:</strong> <span id="swLat"></span> 
            <strong>SouthWest Longitude:</strong> <span id="swLng"></span>
        </p>
        <p class="navbar-text" id="coordinates_circle" style="display:none; color:#ccc;">
            <strong>Latitude:</strong> <span id="crLat"></span>  
            <strong>Longitude:</strong> <span id="crLng"></span> 
            <strong>Radius:</strong> <span id="crRadius"></span>
        </p>
           
		<form class="navbar-form pull-right">
			<label id="cntEquipment" style="color:#eee;"></label>
			<label for="addy-new" style="color:#ccc">Center map on:</label>
			<input type="text" class="form-control map-input" id="map_input" placeholder="Enter Address or Coordinates.." style="width:220px;" >
			<button type="submit" id="map_center_button" class="btn btn-info">Map it</button>
		</form>
    </div>