
var graySquareIcon = L.icon({
  iconUrl: 'images/square-gray-icon.png',
  iconSize: [10, 10]
});
var processPointData = function (groupData, groupStyle, hasObs, constructor)
{
  var rowIndex = 0;
  var rowSize = 4 + (hasObs ? 2 : 0);
  var platformFeatureGroup = [];
  while (rowIndex + rowSize < groupData.length)
  {
    var id = groupData[rowIndex + 0];
    var code = groupData[rowIndex + 1];
    var lat = groupData[rowIndex + 2];
    var lng = groupData[rowIndex + 3];
    var marker = constructor(id, lat, lng, groupStyle, code);

    if (hasObs)
    {
      var metricValue = groupData[rowIndex + 4];
      var englishValue = groupData[rowIndex + 5];
      marker.englishValue = englishValue;
      marker.metricValue = metricValue;
    }
    platformFeatureGroup.push(marker);
    rowIndex += rowSize;
  }
  return platformFeatureGroup;
};

var createCircleMarkers = function (groupData, groupStyle, hasObs)
{
  return processPointData(groupData, groupStyle, hasObs, L.wxdeCircleMarker);
};

var createSquareMarkers = function (groupData, groupStyle, hasObs)
{
  return processPointData(groupData, groupStyle, hasObs, L.wxdeSquareMarker);
};

var processPolylineData = function (groupData, groupStyle)
{
  var rowIndex = 0;
  var rowSize = 7;
  var platformFeatureGroup = [];
  while (rowIndex + rowSize < groupData.length)
  {

    var id = groupData[rowIndex + 0];
    var code = groupData[rowIndex + 1];
    var lat = groupData[rowIndex + 2];
    var lng = groupData[rowIndex + 3];
    var metricValue = groupData[rowIndex + 4];
    var englishValue = groupData[rowIndex + 5];
    var points = groupData[rowIndex + 6];
    var marker = L.wxdePolyline(id, points, new L.LatLng(lat, lng), groupStyle, code);

    marker.englishValue = englishValue;
    marker.metricValue = metricValue;

    platformFeatureGroup.push(marker);
    rowIndex += rowSize;
  }
  return platformFeatureGroup;
};
L.WxdeSummaryMap = L.Map.extend({
  options: {
    latDiv: null,
    lonDiv: null,
    stationCodeDiv: null,
    platformDetailsWindow:
            {
              dialog: null,
              platformDetailsDiv: null,
              platformObsTable: null
            },
    selectedTimeFunction: function ()
    {
      return new Date().getTime();
    },
    selectedObsTypeFunction: function ()
    {
      return 0;
    },
    useMetricUnitsFunction: function ()
    {
      return true;
    }
  },
  initialize: function (id, options)
  {
    L.Map.prototype.initialize.call(this, id, options);
    this._wxdeLayers = [];
    this._minLayerZoom = 18;
    if (this.options.statesLayer)
    {
      var thisMap = this;
      this.addLayer(this.options.statesLayer);

      this.options.statesLayer.eachLayer(
              function (layer)
              {
                layer.on('click',
                        function (e)
                        {

                          thisMap.setView(e.target.getBounds().getCenter(), thisMap.getMinLayerZoom());
                          //thisMap.removeLayer(statesGroup);
                        });
              });
    }

    if (this.options.lstObstypes)
    {
      var thisMap = this;
      var thisObstypeList = $(this.options.lstObstypes);
      $.ajax({
        type: "GET",
        url: "ObsType/list",
        complete: function (data, status)
        {
          var obsList = $.parseJSON(data.responseText);
          for (var rowIndex = 0; rowIndex < obsList.length; ++rowIndex)
          {
            //[{"id":"2001180","name":"canAirTemperature","englishUnits":"F","internalUnits":"C"}
            var obstype = obsList[rowIndex];
            $('<option value="' + obstype.id + '"></option>').appendTo(thisObstypeList).each(function ()
            {
              this.englishUnits = obstype.englishUnits;
              this.internalUnits = obstype.internalUnits;
              this.obstypeName = obstype.name;
            });
          }
          thisMap.updateObstypeLabels();
        },
        timeout: 3000
      });
    }
  },
  updateObstypeLabels: function ()
  {
    if (this.options.lstObstypes)
    {
      var lstObstypes = this.options.lstObstypes;
      var obstypeCount = lstObstypes.length;
      var useMetricLabel = this.useMetricValue();
      for (var obstypeIndex = 1; obstypeIndex < obstypeCount; ++obstypeIndex)
      {
        var obstypeOption = lstObstypes[obstypeIndex];
        var obstypeName = obstypeOption.obstypeName;
        if (!obstypeName)
          continue;

        var unitLabel = useMetricLabel ? obstypeOption.internalUnits : obstypeOption.englishUnits;

        if (unitLabel)
          $(obstypeOption).text(obstypeName + ' (' + unitLabel + ')');
        else
          $(obstypeOption).text(obstypeName);

      }

    }
  },
  updateObsValueUnits: function ()
  {
    var layerCount = this._wxdeLayers.length;
    var thisMap = this;
    for (var layerIndex = 0; layerIndex < layerCount; ++layerIndex)
    {
      this._wxdeLayers[layerIndex].eachLayer(function (layer)
      {
        if (layer.obsMarker)
          layer.obsMarker.setText(thisMap.useMetricValue() ? layer.metricValue : layer.englishValue);
      }, this._wxdeLayers[layerIndex]);
    }
  },
  registerWxdeLayer: function (layer)
  {
    this._wxdeLayers.push(layer);
    layer.setMap(this);
  },
  getWxdeLayers: function ()
  {
    return this._wxdeLayers;
  },
  refreshLayers: function ()
  {
    var layerCount = this._wxdeLayers.length;
    for (var layerIndex = 0; layerIndex < layerCount; ++layerIndex)
    {
      this._wxdeLayers[layerIndex].refreshData();
    }
    //thisLayer.refreshData();
  },
  reorderLayerElements: function ()
  {
    var layerCount = this._wxdeLayers.length;
    for (var layerIndex = 0; layerIndex < layerCount; ++layerIndex)
    {
      if (this.hasLayer(this._wxdeLayers[layerIndex]))
      {
        this._wxdeLayers[layerIndex].eachLayer(function (layer)
        {
          if (layer.bringToBack)
            layer.bringToBack();
        }, this._wxdeLayers[layerIndex]);
      }
    }
  },
  getMinLayerZoom: function ()
  {
    return this._minLayerZoom;
  },
  getSelectedTime: function ()
  {
    return this.options.selectedTimeFunction();
  },
  useMetricValue: function ()
  {
    return this.options.useMetricUnitsFunction();
  },
  getSelectedObsType: function ()
  {
    return this.options.selectedObsTypeFunction();

  }
});
L.wxdeSummaryMap = function (id, options)
{
  return new L.WxdeSummaryMap(id, options);
};
L.WxdeLayer = L.LayerGroup.extend({
  options: {
    checkbox: null, // checkbox input used to enable/disable layer
    highlightFillColor: "yellow",
    hasObs: true,
    platformDetailsFunction: function (marker)
    {
      var details = [];
      details.sc = marker.getStationCode();
      //  details.id = marker.getPlatformId();

      var latLng = marker.getLatLng();
      details.lat = latLng.lat.toFixed(6);
      details.lng = latLng.lng.toFixed(6);

      return details;
    },
    obsRequestBoundsFunction: function (marker)
    {
      return marker.requestBounds;
    }
  },
  initialize: function (baseUrl, layerParser, layerMarkerStyle, options)
  {
    L.LayerGroup.prototype.initialize.call(this, null);
    L.setOptions(this, options);
    this._baseUrl = baseUrl;
    this._layerParser = layerParser;
    this._layerMarkerStyle = layerMarkerStyle;
    this._zoomLayers = [];
    this._zoomRequests = [];
    if (this.options.checkbox)
    {
      this._checkbox = this.options.checkbox;
      var thisLayer = this;
      $(this._checkbox).change(function ()
      {
        if (!this.checked && thisLayer._wxdeMap.hasLayer(thisLayer))
          thisLayer._wxdeMap.removeLayer(thisLayer);
        else if (this.checked)
        {
          thisLayer._wxdeMap.addLayer(thisLayer);
          if (thisLayer.isEnabled(thisLayer._wxdeMap.getZoom()))
            thisLayer.refreshData(true);
        }
      });
    }

    if (this.options.highlightFillColor)
    {
      var highlightStyle = {};
      for (var styleAttr in layerMarkerStyle)
      {
        if (layerMarkerStyle.hasOwnProperty(styleAttr))
          highlightStyle[styleAttr] = layerMarkerStyle[styleAttr];
      }
      highlightStyle.color = this.options.highlightFillColor;
      this._highlightStyle = highlightStyle;
    }

  },
  getPlatformDetails: function (marker)
  {
    return this.options.platformDetailsFunction(marker);
  },
  isEnabled: function (zoom)
  {
    return zoom >= this._minZoom;
  },
  isUserSelected: function ()
  {
    return !this._checkbox || this._checkbox.checked;
  },
  _getZoomLayer: function (zoom)
  {
    var zoomLayer = this._zoomLayers[zoom];
    if (!zoomLayer)
    {
      zoomLayer = new L.LayerGroup();
      this.addLayer(zoomLayer);
      this._zoomLayers[zoom] = zoomLayer;
    }
    return zoomLayer;
  },
  _getZoomRequest: function (zoom)
  {
    return this._zoomRequests[zoom];
  },
  _getMarkerObsRequestBounds: function (marker)
  {
    return this.options.obsRequestBoundsFunction(marker);
  },
  _hasObs: function ()
  {
    return this.options.hasObs;
  },
  eachLayer: function (method, context)
  {
    if (!this._zoomLevels)
      return this;
    for (var zoomIndex = 0; zoomIndex < this._zoomLevels.length; ++zoomIndex)
    {
      this._getZoomLayer(this._zoomLevels[zoomIndex]).eachLayer(method, context);
    }
    return this;
  },
  setMap: function (map)
  {
    this._wxdeMap = map;
    var thisLayer = this;
    this._wxdeMap.on('dragend', function (event)
    {
      thisLayer.refreshData();
    });
    this._wxdeMap.on('zoomend', function (event)
    {
      if (map.options.statesLayer)
      {
        var statesLayer = map.options.statesLayer;
        var zoom = map.getZoom();
        var breakpointZoom = map.getMinLayerZoom() - 1;
        var onMap = map.hasLayer(statesLayer);
        if (zoom > breakpointZoom && onMap)
        {
          map.removeLayer(statesLayer);
        }
        else if (zoom <= breakpointZoom && !onMap)
        {
          map.addLayer(statesLayer);
        }
      }

      if (thisLayer.isEnabled(this.getZoom()))
      {
        if (thisLayer._checkbox)
          thisLayer._checkbox.disabled = false;
        if (thisLayer.isUserSelected())
        {
          if (!this.hasLayer(thisLayer))
            this.addLayer(thisLayer);
          thisLayer.refreshData();
        }
      }
      else
      {
        if (this.hasLayer(thisLayer))
          this.removeLayer(thisLayer);
        if (thisLayer._checkbox)
          thisLayer._checkbox.disabled = true;
      }

    });
    if (map.options.stationCodeDiv || this._highlightStyle)
    {
      var stationDiv = map.options.stationCodeDiv;
      var highlightStyle = this._highlightStyle;
      var originalStyle = this._layerMarkerStyle;
      this._markerMouseOver = function (event)
      {
        if (highlightStyle && this.setStyle)
          this.setStyle(highlightStyle);
        stationDiv.innerHTML = this.getStationCode();
      };
      this._markerMouseOut = function (event)
      {
        if (highlightStyle && this.setStyle)
          this.setStyle(originalStyle);
        stationDiv.innerHTML = '';
      };
    }

    if (map.options.platformDetailsWindow)
    {
      var thisDetailsWindow = map.options.platformDetailsWindow;
      this._markerMouseClick = function (event)
      {
        var obsTable = $(thisDetailsWindow.platformObsTable);
        //mae table visible or invisible based on _hasobs
        var QCH_MAX = 15;
        var colCount = QCH_MAX + 6;


        var platformDetails = thisLayer.getPlatformDetails(this);

        var detailsDiv = thisDetailsWindow.platformDetailsDiv;

        var detailsContent = '';

        detailsContent += platformDetails.sc + '<br />';

        detailsContent += 'Lat, Lon: ' + platformDetails.lat;
        detailsContent += ', ' + platformDetails.lng + '<br />';

        detailsDiv.html(detailsContent);

        var bounds = thisLayer._getMarkerObsRequestBounds(this);
        obsTable.show();
        obsTable.find('tbody > tr').remove();
        obsTable.find('tbody:last-child').append('<tr><td colspan="' + colCount + '">Loading data...</td></tr>');



        $.ajax({
          type: "GET",
          url: thisLayer._baseUrl + "/platformObs/" + this.getPlatformId() + "/" + thisLayer._wxdeMap.getSelectedTime() + "/" + bounds.getNorth() + "/" + bounds.getWest() + "/" + bounds.getSouth() + "/" + bounds.getEast(),
          complete: function (data, status)
          {
            if (data.responseText === '')
            {
              obsTable.find('tbody > tr').remove();
              obsTable.find('tbody:last-child').append('<tr><td colspan="' + colCount + '">Error loading data</td></tr>');
              return;
            }
            var additionalDetails = $.parseJSON(data.responseText);

            detailsContent = '';

            detailsContent += platformDetails.sc + '<br />';

            if (additionalDetails.tnm)
              detailsContent += additionalDetails.tnm + '<br />';

            detailsContent += 'Lat, Lon: ' + platformDetails.lat;
            detailsContent += ', ' + platformDetails.lng + '<br />';
            if (additionalDetails.tel)
              detailsContent += 'Elevation: ' + additionalDetails.tel;

            detailsDiv.html(detailsContent);


            var obsList = additionalDetails.obs;

            obsTable.find('tbody > tr').remove();

            if (!obsList || obsList.length === 0)
            {
              obsTable.find('tbody:last-child').append('<tr><td colspan="' + colCount + '">No data</td></tr>');
            }
            else
            {


              var m_oQCh =
                      [
                        [{im: "b", tx: ""}, {im: "nr", tx: "not run"}],
                        [{im: "n", tx: "not passed"}, {im: "p", tx: "passed"}]
                      ];


              var newRows = '';
              for (var rowIndex = 0; rowIndex < obsList.length; ++rowIndex)
              {
                var iObs = obsList[rowIndex];
                newRows += '<tr>';
                newRows += "<td class=\"timestamp\">" + iObs.ts + "</td>\n";
                newRows += "<td class=\"obsType\">" + iObs.ot + "</td>\n";
                newRows += "<td class=\"td-ind\">" + iObs.si + "</td>\n";

                newRows += "<td class=\"td-value\">";
                if (thisLayer._wxdeMap.useMetricValue())
                  newRows += iObs.mv;
                else
                  newRows += iObs.ev;
                newRows += "</td>\n";

                newRows += "<td class=\"unit\">";
                var unit;
                if (thisLayer._wxdeMap.useMetricValue())
                  unit = iObs.mu;
                else
                  unit = iObs.eu;

                if (unit)
                  newRows += unit;
                newRows += "</td>\n";


                newRows += "<td class=\"conf\">" + 100.00 * iObs.cv + "%</td>\n";

                var sRun = Number(iObs.rf).toString(2);
                while (sRun.length < QCH_MAX)
                  sRun = "0" + sRun;

                var sPass = Number(iObs.pf).toString(2);
                while (sPass.length < QCH_MAX)
                  sPass = "0" + sPass;

                var nIndex = QCH_MAX;
                while (--nIndex >= 0)
                {
                  var nRow = Number(sRun.charAt(nIndex));
                  var nCol = Number(sPass.charAt(nIndex));
                  var oFlag = m_oQCh[nRow][nCol];

                  newRows += "    <td><img alt='Icon' src=\"image/";
                  newRows += oFlag.im;
                  newRows += ".png\" alt=\"";
                  newRows += oFlag.tx;
                  newRows += "\"/></td>\n";
                }

                newRows += '</tr>';
              }

              obsTable.find('tbody:last-child').append(newRows);
            }


            //    thisDetailsWindow.dialog.dialog("open");

            thisDetailsWindow.dialog.resize();
            thisDetailsWindow.dialog.dialog("option", "position", "center");
          },
          error: function (XMLHttpRequest, textStatus, errorThrown)
          {
            obsTable.find('tbody > tr').remove();
            obsTable.find('tbody:last-child').append('<tr><td colspan="' + colCount + '">Error loading data</td></tr>');
          },
          timeout: 10000
        });

        //     if (!thisLayer._hasObs())
        {
          thisDetailsWindow.dialog.dialog("open");
        }

      };
    }

    if (map.options.latDiv && map.options.lngDiv)
    {
      var latDiv = map.options.latDiv;
      var lngDiv = map.options.lngDiv;
      map.on('mousemove', function (e)
      {
        latDiv.innerHTML = e.latlng.lat.toFixed(6);
        lngDiv.innerHTML = e.latlng.lng.toFixed(6);
      });
    }


    var thisLayer = this;
    $.ajax({
      type: "GET",
      url: this._baseUrl + "/GetZoomLevels",
      complete: function (data, status)
      {
        var zoomLevels = $.parseJSON(data.responseText);
        thisLayer._zoomLevels = zoomLevels;
        zoomLevels.sort(function (a, b)
        {
          return a - b;
        });
        thisLayer._minZoom = zoomLevels[0];
        map._minLayerZoom = Math.min(map._minLayerZoom, thisLayer._minZoom);
        for (var zoomIndex = 0; zoomIndex < zoomLevels.length; ++zoomIndex)
        {
          var zoomLevel = zoomLevels[zoomIndex];
          var zoomRequest = L.platformRequest(0, 0, L.latLngBounds(L.latLng(0, 0), L.latLng(0, 0)));
          thisLayer._zoomRequests[zoomLevel] = zoomRequest;
        }
      },
      timeout: 3000
    });
  },
  refreshData: function (firstLoad)
  {
    if (this._wxdeMap)
    {
      var selectedTime = this._wxdeMap.getSelectedTime();
      var selectedObsType = this._wxdeMap.getSelectedObsType();
      var bounds = this._wxdeMap.getBounds().pad(.5);
      var currentZoom = this._wxdeMap.getZoom();
      var requestData = false;
      var highestValidZoomIndex = -1;

      if (!this.isEnabled(currentZoom) || !this.isUserSelected())
        return;

      for (var zoomIndex = 0; zoomIndex < this._zoomLevels.length; ++zoomIndex)
      {
        var zoomLevel = this._zoomLevels[zoomIndex];
        var zoomLayer = this._getZoomLayer(zoomLevel);
        var zoomLevelRequest = this._getZoomRequest(zoomLevel);
        if (zoomLevel > currentZoom)
        {
          if (this.hasLayer(zoomLayer))
            this.removeLayer(zoomLayer);
          if (zoomIndex - highestValidZoomIndex > 1)
          {
            //if it's more than one zoom level above the current zoom level, drop all points
            zoomLayer.clearLayers();
            zoomLevelRequest.clearValues();
          }
        }
        else
        {

          highestValidZoomIndex = zoomIndex;
          //make sure the layer is currently on the map
          if (!this.hasLayer(zoomLayer))
            this.addLayer(zoomLayer);

          //if this layer doesn't have obs the time and obstype don't affect what layer elements are returned
          //if it does have obs changing the type or time will clear the cached elements
          if (this._hasObs() && (zoomLevelRequest.obsType !== selectedObsType || zoomLevelRequest.timestamp !== selectedTime))
          {
            zoomLevelRequest.clearValues();
            zoomLayer.clearLayers();
          }
          else if (zoomLevelRequest.latLngBounds.contains(bounds))
            continue;

          requestData = true;
          zoomLevelRequest.setBoundaryValues(bounds);
          zoomLevelRequest.timestamp = selectedTime;
          zoomLevelRequest.obsType = selectedObsType;

          var zoomLayer = this._getZoomLayer(zoomLevel);
          zoomLayer.eachLayer(function (layer)
          {
            if (!layer.intersects(bounds))
              zoomLayer.removeLayer(layer);
          });
        }

      }

      //drop cached data from high zoom levels if we have zoomed out
      //far enough

      //drop cached points from the highest zoom level
      //we have data for if we are scrolling around and the current boundary
      //isn't contained by the boundary that we have data for. If it
      // is contained, then we keep working with what we have cached. If it
      //just intersects or is entirely outside of what we have cached, then
      //we will get new points and drop whatever is outside if the current
      //boundary.

      //Check if there is a zoom level we don't already have the data for
      //before requesting new data


      if (requestData)
      {
        var thisLayer = this;
        $.ajax({
          type: "GET",
          url: this._baseUrl + "/" + selectedTime + "/" + currentZoom + "/" + bounds.getNorth() + "/" + bounds.getWest() + "/" + bounds.getSouth() + "/" + bounds.getEast() + "/" + selectedObsType,
          complete: function (data, status)
          {
            var zoomLayers = $.parseJSON(data.responseText);
            var hasMarkerMouseOverEvents = (thisLayer._markerMouseOver && thisLayer._markerMouseOut) ? true : false;
            for (var zoomLevel in zoomLayers)
            {
              if (!zoomLayers.hasOwnProperty(zoomLevel))
                continue;
              var newLayers = thisLayer._layerParser(zoomLayers[zoomLevel], thisLayer._layerMarkerStyle, thisLayer._hasObs());
              var zoomLayer = thisLayer._getZoomLayer(zoomLevel);
              for (var layerIndex = 0; layerIndex < newLayers.length; ++layerIndex)
              {
                var layer = newLayers[layerIndex];

                var value = thisLayer._wxdeMap.useMetricValue() ? layer.metricValue : layer.englishValue;
                zoomLayer.addLayer(layer);
                layer.requestBounds = bounds;
                if (value)
                {
                  var obsMarker = L.wxdeObsMarker(layer.getLatLng(), value);
                  layer.obsMarker = obsMarker;
                  zoomLayer.addLayer(obsMarker);
                }
                if (hasMarkerMouseOverEvents)
                {
                  layer.on('mouseover', thisLayer._markerMouseOver);
                  layer.on('mouseout', thisLayer._markerMouseOut);
                }
                if (thisLayer._markerMouseClick)
                  layer.on('click', thisLayer._markerMouseClick);
              }
            }

            // if (firstLoad)
            thisLayer._wxdeMap.reorderLayerElements();
          },
          timeout: 30000
        });
      } // this will be if a map layer is removed, but added back wiithout panning
      else if (firstLoad)
        this._wxdeMap.reorderLayerElements();
    }
  }
});
L.wxdeLayer = function (baseUrl, minZoom, layerParser, layerMarkerStyle, options)
{
  return new L.WxdeLayer(baseUrl, minZoom, layerParser, layerMarkerStyle, options);
};
L.PlatformRequest = L.LayerGroup.extend({
  initialize: function (timestamp, obsType, latLngBounds)
  {
    this.timestamp = timestamp;
    this.obsType = obsType;
    this.latLngBounds = latLngBounds;
  },
  clearValues: function ()
  {
    this.latLngBounds.getSouthWest().lat = 0;
    this.latLngBounds.getSouthWest().lng = 0;
    this.latLngBounds.getNorthEast().lat = 0;
    this.latLngBounds.getNorthEast().lng = 0;
    this.timestamp = 0;
    this.obsType = 0;
  },
  setBoundaryValues: function (bounds)
  {
    this.latLngBounds.getSouthWest().lat = bounds.getSouthWest().lat;
    this.latLngBounds.getSouthWest().lng = bounds.getSouthWest().lng;
    this.latLngBounds.getNorthEast().lat = bounds.getNorthEast().lat;
    this.latLngBounds.getNorthEast().lng = bounds.getNorthEast().lng;
  }

});
L.platformRequest = function (zoom, timestamp, latLngBounds)
{
  return new L.PlatformRequest(zoom, timestamp, latLngBounds);
};

L.WxdeCircleMarker = L.CircleMarker.extend({
  options: {
    latlngDiv: null,
    stationCodeDiv: null
  },
  initialize: function (id, lat, lng, style, stationCode, options)
  {
    L.CircleMarker.prototype.initialize.call(this, new L.LatLng(lat, lng), style, options);
    this._stationCode = stationCode;
    this._platformId = id;
  },
  getPlatformId: function ()
  {
    return this._platformId;
  },
  getStationCode: function ()
  {
    return this._stationCode;
  },
  intersects: function (bounds)
  {
    return bounds.contains(this.getLatLng());
  }
});
L.wxdeCircleMarker = function (id, lat, lng, style, stationCode, options)
{
  return new L.WxdeCircleMarker(id, lat, lng, style, stationCode, options);
};


L.WxdeSquareMarker = L.Marker.extend({
  options: {
    latlngDiv: null,
    stationCodeDiv: null
  },
  initialize: function (id, lat, lng, stationCode, options)
  {
    L.Marker.prototype.initialize.call(this, new L.LatLng(lat, lng), options);
    this._stationCode = stationCode;
    this._platformId = id;
  },
  getPlatformId: function ()
  {
    return this._platformId;
  },
  getStationCode: function ()
  {
    return this._stationCode;
  },
  intersects: function (bounds)
  {
    return bounds.contains(this.getLatLng());
  }
});
L.wxdeSquareMarker = function (id, lat, lng, style, stationCode, options)
{
  if (!options)
    options = [];
  options.icon = graySquareIcon;
  return new L.WxdeSquareMarker(id, lat, lng, stationCode, options);
};

L.WxdeObsMarker = L.Marker.extend({
  initialize: function (platformLatlng, value, options)
  {
    if (!options)
      options = {};
    options.icon = L.divIcon({html: value, iconSize: '', className: 'obs-marker-icon'});
    L.CircleMarker.prototype.initialize.call(this, platformLatlng, options);
  },
  intersects: function (bounds)
  {
    return bounds.contains(this.getLatLng());
  },
  setText: function (text)
  {
    $(this._icon).text(text);
  }

});
L.wxdeObsMarker = function (platformLatlng, value, options)
{
  return new L.WxdeObsMarker(platformLatlng, value, options);
};


L.WxdePolyline = L.Polyline.extend({
  options: {
    latlngDiv: null,
    stationCodeDiv: null
  },
  initialize: function (id, latlngs, midLatLng, style, stationCode, options)
  {

    this.midLatLng = midLatLng;
    if (!options)
      options = {};
    for (var styleProperty in style)
    {
      if (style.hasOwnProperty(styleProperty))
        options[styleProperty] = style[styleProperty];
    }
    L.Polyline.prototype.initialize.call(this, latlngs, options);
    this._stationCode = stationCode;
    this._platformId = id;
  },
  getPlatformId: function ()
  {
    return this._platformId;
  },
  getStationCode: function ()
  {
    return this._stationCode;
  },
  intersects: function (bounds)
  {
    return this.getBounds().intersects(bounds);
  },
  getLatLng: function ()
  {
    return this.midLatLng;
  }
});
L.wxdePolyline = function (id, latlngs, midLatLng, style, stationCode, options)
{
  return new L.WxdePolyline(id, latlngs, midLatLng, style, stationCode, options);
};



