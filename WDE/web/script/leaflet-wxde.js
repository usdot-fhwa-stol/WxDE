
var graySquareIcon = L.icon({
  iconUrl: 'images/square-gray-icon.png',
  iconSize: [10, 10]
});

function StaticLayerStyler(style)
{
  this.style = style;
}

StaticLayerStyler.prototype.styleLayer = function (layer)
{
  if (layer.setStyle)
    layer.setStyle(this.style);
};

function RoadStatusStyler(statusStyles, map, highlighter)
{
  this.statusStyles = statusStyles;
  this.highlighter = highlighter;
  this.map = map;
}

RoadStatusStyler.prototype.styleLayer = function (layer)
{
  var highlightLines = this.highlighter.getHighlightLines();
  var lineIndex = highlightLines.length;
  while (--lineIndex >= 0)
  {
    var line = highlightLines.pop();
    this.map.removeLayer(line);
  }

  if (layer.setStyle)
    layer.setStyle(this.statusStyles[layer.options.status]);
};

function RoadHighlighter(highlightStyle, map)
{
  this.style = highlightStyle;
  this.map = map;
  this.highlightLines = [];
}


RoadHighlighter.prototype.styleLayer = function (layer)
{
  var highlightLine = L.polyline(layer.getLatLngs(), this.style).addTo(this.map);
  highlightLine.bringToBack();
  this.highlightLines.push(highlightLine);
};

RoadHighlighter.prototype.getHighlightLines = function ()
{
  return this.highlightLines;
};


var processPointData = function (groupData, hasObs, constructor)
{
  var rowIndex = 0;
  var rowSize = 4 + (hasObs ? 2 : 0);
  var platformFeatureGroup = [];
  while (rowIndex + rowSize <= groupData.length)
  {
    var id = groupData[rowIndex + 0];
    var code = groupData[rowIndex + 1];
    var lat = groupData[rowIndex + 2];
    var lng = groupData[rowIndex + 3];
    var marker = constructor(id, lat, lng, code);

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

var createCircleMarkers = function (groupData, hasObs)
{
  return processPointData(groupData, hasObs, L.wxdeCircleMarker);
};

var createSquareMarkers = function (groupData, hasObs)
{
  return processPointData(groupData, hasObs, L.wxdeSquareMarker);
};

var processPolylineData = function (groupData)
{
  var rowIndex = 0;
  var rowSize = 8;
  var platformFeatureGroup = [];
  while (rowIndex + rowSize <= groupData.length)
  {

    var id = groupData[rowIndex + 0];
    var code = groupData[rowIndex + 1];
    var lat = groupData[rowIndex + 2];
    var lng = groupData[rowIndex + 3];
    var status = groupData[rowIndex + 4];
    var metricValue = groupData[rowIndex + 5];
    var englishValue = groupData[rowIndex + 6];
    var points = groupData[rowIndex + 7];

    var marker = L.wxdePolyline(id, points, new L.LatLng(lat, lng), code, {status: status});

    marker.englishValue = englishValue;
    marker.metricValue = metricValue;

    platformFeatureGroup.push(marker);
    rowIndex += rowSize;
  }
  return platformFeatureGroup;
};

/**
 function LayerDetailsManager(thead)
 {
 this.thead = thead;
 }

 LayerDetailsManager.prototype.getThead = function()
 {
 return this.thead;
 };



 LayerDetailsManager.prototype.generateDetailRows = function(layerDetails)
 {

 };
 **/


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
      this.setStatesLayer(this.options.statesLayer);

    }


    $("#road-legend-form").dialog({
      autoOpen: false,
      modal: true,
      draggable: false,
      resizable: false,
      width: "400",
      height: "auto",
      position: {my: "center", at: "center"}
    });


    $("#details-form").dialog({
      autoOpen: false,
      modal: true,
      draggable: false,
      resizable: false,
      width: "400",
      height: "auto",
      position: {my: "center", at: "center"}
    });

    $("#summary-legend-form").dialog({
      autoOpen: false,
      modal: true,
      draggable: false,
      resizable: false,
      width: "400",
      height: "auto",
      position: {my: "center", at: "center"}
    });

    $(window).resize(function ()
    {
      var position = {my: "center", at: "center", of: window};
      $("#road-legend-form, #details-form, #summary-legend-form").each(function (index)
      {
        if ($(this).hasClass("ui-dialog-content")) // Test if the dialog has been initialized
          $(this).dialog("option", "position", position);
      });
    });

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

          $('<option value="StationCode"></option>').appendTo(thisObstypeList).each(function ()
          {
            this.englishUnits = '';
            this.internalUnits = '';
            this.obstypeName = 'Station Code';
          });

          thisMap.updateObstypeLabels();
        },
        timeout: 3000
      });
    }
  },
  showDialog: function (alwaysShow)
  {
    var zoom = this.getZoom();

    var firstLayerZoom = this.getMinLayerZoom();
    var roadLayerZoom = 11;
    if (zoom >= roadLayerZoom)
    {
      if (!this.roadDialogShown || alwaysShow)
      {
        $("#details-form").dialog("close");
        $("#summary-legend-form").dialog("close");

        this.roadDialogShown = true;
        $("#road-legend-form").dialog("open");
      }

    }
    else if (zoom >= firstLayerZoom)
    {
      if (!this.detailDialogShown || alwaysShow)
      {
        $("#road-legend-form").dialog("close");
        $("#summary-legend-form").dialog("close");

        this.detailDialogShown = true;
        $("#details-form").dialog("open");
      }
    }
    else
    {
      if (!this.summaryDialogShown || alwaysShow)
      {
        $("#road-legend-form").dialog("close");
        $("#details-form").dialog("close");

        this.summaryDialogShown = true;

        $("#summary-legend-form").dialog("open");
      }
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
  showStationCodeLabels: function ()
  {
    this.hasStationCodeLabels = true;
    this.eachWxdeLayer(function (wxdeLayer)
    {
      if (wxdeLayer.showObsLabels())
      {
        wxdeLayer.eachZoomLayer(function (zoomLayer)
        {
          zoomLayer.eachLayer(function (layer)
          {
            if (layer.obsMarker)
            {
              layer.obsMarker.setText(layer.getStationCode());
              if (!zoomLayer.hasLayer(layer.obsMarker))
                zoomLayer.addLayer(layer.obsMarker);
            }
            else
            {
              var obsMarker = L.wxdeObsMarker(layer.getLatLng(), layer.getStationCode());
              layer.obsMarker = obsMarker;
              zoomLayer.addLayer(obsMarker);
            }
          });
        });
      }
    });
  },
  hideLayerDivs: function ()
  {
    this.hasStationCodeLabels = false;
    this.eachWxdeLayer(function (wxdeLayer)
    {
      wxdeLayer.eachZoomLayer(function (zoomLayer)
      {
        zoomLayer.eachLayer(function (layer)
        {
          if (layer.obsMarker)
          {
            zoomLayer.hasLayer(layer.obsMarker);
            zoomLayer.removeLayer(layer.obsMarker);
          }
        });
      });
    });
  },
  eachWxdeLayer: function (method, context)
  {
    if (!this._wxdeLayers)
      return this;
    var layerCount = this._wxdeLayers.length;
    for (var layerIndex = 0; layerIndex < layerCount; ++layerIndex)
    {
      method.call(context, this._wxdeLayers[layerIndex]);
    }
    return this;
  }
  ,
  registerWxdeLayer: function (layer)
  {
    this._wxdeLayers.push(layer);
    layer.setMap(this);
  }
  ,
  getsummary: function ()
  {
    return this._wxdeLayers;
  }
  ,
  refreshLayers: function ()
  {
    var layerCount = this._wxdeLayers.length;
    for (var layerIndex = 0; layerIndex < layerCount; ++layerIndex)
    {
      this._wxdeLayers[layerIndex].refreshData();
    }
    //thisLayer.refreshData();
  }
  ,
  reorderLayerElements: function ()
  {
    var layerCount = this._wxdeLayers.length;
    for (var layerIndex = 0; layerIndex < layerCount; ++layerIndex)
    {
      if (this.hasLayer(this._wxdeLayers[layerIndex]))
      {
        this._wxdeLayers[layerIndex].bringToBack();
      }
    }
  }
  ,
  getMinLayerZoom: function ()
  {
    return this._minLayerZoom;
  }
  ,
  setStatesLayer: function (statesLayer)
  {
    var thisMap = this;
    this.statesLayer = statesLayer;
    this.addLayer(statesLayer);



    this.on('zoomend', function (event)
    {
      var zoom = thisMap.getZoom();
      var breakpointZoom = thisMap.getMinLayerZoom() - 1;
      var onMap = thisMap.hasLayer(statesLayer);

      thisMap.showDialog();

      if (zoom > breakpointZoom && onMap)
      {
        thisMap.removeLayer(statesLayer);
      }
      else if (zoom <= breakpointZoom && !onMap)
      {
        thisMap.addLayer(statesLayer);
      }

      var disabled = zoom > breakpointZoom ? false : true;

      $('.disableOnSummary').each(function (idx, el)
      {
        el.disabled = disabled;
        if (disabled)
          $(el).addClass('DisabledElement');
        else
          $(el).removeClass('DisabledElement');
      });


    });


    statesLayer.eachLayer(
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
  ,
  getStatesLayer: function ()
  {
    return this.statesLayer;
  }
  ,
  getSelectedTime: function ()
  {
    return this.options.selectedTimeFunction();
  }
  ,
  useMetricValue: function ()
  {
    return this.options.useMetricUnitsFunction();
  }
  ,
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
    hasObs: true,
    showObsLabels: true,
    isForecastOnly: false,
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
  initialize: function (baseUrl, layerParser, layerStyler, options)
  {
    L.LayerGroup.prototype.initialize.call(this, null);
    L.setOptions(this, options);
    this._baseUrl = baseUrl;
    this._layerParser = layerParser;
    this.layerStyler = layerStyler;
    this._zoomLayers = [];
    this._zoomRequests = [];
    this._highlighter = this.options.highlighter;
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
          if (thisLayer.isEnabled(thisLayer._wxdeMap.getZoom(), thisLayer._wxdeMap.getSelectedTime()))
            thisLayer.refreshData(true);
        }
      });
    }

  },
  isForecastOnly: function ()
  {
    return this.options.isForecastOnly;
  },
  showObsLabels: function ()
  {
    return this.options.showObsLabels;
  },
  bringToBack: function ()
  {
    this.eachLayer(function (layer)
    {
      if (layer.bringToBack)
        layer.bringToBack();
    }, this);
  },
  getPlatformDetails: function (marker)
  {
    return this.options.platformDetailsFunction(marker);
  },
  isEnabled: function (zoom, requestTime)
  {
    var enabled = zoom >= this._minZoom;

    if (enabled && requestTime && (this.options.enabledForTime))
    {
      return this.options.enabledForTime(requestTime);
    }

    return enabled;
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
  eachZoomLayer: function (method, context)
  {
    if (!this._zoomLevels)
      return this;
    for (var zoomIndex = 0; zoomIndex < this._zoomLevels.length; ++zoomIndex)
    {
      method.call(context, this._getZoomLayer(this._zoomLevels[zoomIndex]));
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
      thisLayer.refreshData();
    });
    if (map.options.stationCodeDiv || this._highlighter)
    {
      var stationDiv = map.options.stationCodeDiv;
      this._markerMouseOver = function (event)
      {
        if (thisLayer._highlighter)
          thisLayer._highlighter.styleLayer(this);
        stationDiv.innerHTML = this.getStationCode();
      };
      this._markerMouseOut = function (event)
      {
        if (thisLayer._highlighter)
          thisLayer.layerStyler.styleLayer(this);
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
        var obsThead =
                '\n<tr align="center">\n' +
                '<td class="td-title" colspan="6"><div id="platform-details"> </div>\n' +
                '</td>\n' +
                '<td rowspan="2" class="td-image no-border-left webkit-td-image-fix"><img alt="Complete" src="image/qch/Complete.png"></td>\n' +
                '<td rowspan="2" class="td-image"><img alt="Manual" src="image/qch/Manual.png"></td>\n' +
                '<td rowspan="2" class="td-image"><img alt="Sensor Range" src="image/qch/SensorRange.png"></td>\n' +
                '<td rowspan="2" class="td-image"><img alt="Climate Range" src="image/qch/ClimateRange.png"></td>\n' +
                '<td rowspan="2" class="td-image"><img alt="Step" src="image/qch/Step.png"></td>\n' +
                '<td rowspan="2" class="td-image"><img alt="Like Instrument" src="image/qch/LikeInstrument.png"></td>\n' +
                '<td rowspan="2" class="td-image"><img alt="Persistence" src="image/qch/Persistence.png"></td>\n' +
                '<td rowspan="2" class="td-image"><img alt="Inter-quartile Range" src="image/qch/IQR.png"></td>\n' +
                '<td rowspan="2" class="td-image"><img alt="Barnes Spatial" src="image/qch/BarnesSpatial.png"></td>\n' +
                '<td rowspan="2" class="td-image"><img alt="Dewpoint" src="image/qch/Dewpoint.png"></td>\n' +
                '<td rowspan="2" class="td-image"><img alt="Sea Level Pressure" src="image/qch/SeaLevelPressure.png"></td>\n' +
                '<td rowspan="2" class="td-image"><img alt="Accumulated Precipitation" src="image/qch/PrecipAccum.png"></td>\n' +
                '<td rowspan="2" class="td-image"><img src="image/qch/ModelAnalysis.png" alt="Model Analysis"></td>\n' +
                '<td class="td-image" rowspan="2"><img src="image/qch/NeighboringVehicle.png" alt="Neighboring Vehicle"></td>\n' +
                '<td class="td-image" rowspan="2"><img src="image/qch/VehicleStdDev.png" alt="Vehicle Standard Deviation"></td>\n' +
                '</tr>\n' +
                '<tr class="last-tr">\n' +
                '<td class="timestamp"><b>Timestamp</b></td>\n' +
                '<td class="obsType"><b>Observation Type</b></td>\n' +
                '<td class="td-ind"><b>Ind</b></td>\n' +
                '<td class="td-value"><b>Value</b></td>\n' +
                '<td class="unit"><b>Unit</b></td>\n' +
                '<td class="conf webkit-td-conf-fix"><b>Conf</b></td>\n' +
                '</tr>';
        var forecastThead =
                '\n<tr align="center">\n' +
                '<td class="td-title"><div id="platform-details"> </div>\n' +
                '</td>\n' +
                '</tr>\n' +
                '<tr class="last-tr">\n' +
                '<td class="timestamp"><b>Timestamp</b></td>\n' +
                '<td class="obsType"><b>Observation Type</b></td>\n' +
                '<td class="td-value"><b>Value</b></td>\n' +
                '<td class="unit"><b>Unit</b></td>\n' +
                '</tr>';
        var sensorThead =
                '\n<tr align="center">\n' +
                '<td class="td-title" colspan="2"><div id="platform-details"> </div>\n' +
                '</td>\n' +
                '</tr>\n' +
                '<tr class="last-tr">\n' +
                '<td class="obsType"><b>Observation Type</b></td>\n' +
                '<td class="sensorIndex"><b>Index</b></td>\n' +
                '<td class="sensorMake"><b>Make</b></td>\n' +
                '<td class="sensorModel"><b>Model</b></td>\n' +
                '</tr>';
        obsTable.find('thead > tr').remove();
        var colCount;
        if (thisLayer._hasObs())
        {
          var thead;
          if (thisLayer.isForecastOnly())
            thead = forecastThead;
          else
            thead = obsThead;

          obsTable.find('thead').append(thead);
          colCount = QCH_MAX + 6;
        }
        else
        {
          obsTable.find('thead').append(sensorThead);
          colCount = 2;
        }

        var closeDetailsFn = function ()
        {
          thisDetailsWindow.dialog.dialog("close");
        };

        var platformDetails = thisLayer.getPlatformDetails(this);
        var detailsDiv = obsTable.find('#platform-details');
        var buttonElement = '<button type="button" class="ui-button ui-widget ui-state-default ui-corner-all ui-button-icon-only no-title-form ui-dialog-titlebar-close" role="button" title="Close"><span class="ui-button-icon-primary ui-icon ui-icon-closethick"></span><span class="ui-button-text">Close</span></button>';
        var detailsContent = buttonElement + '<br />';
        detailsContent += platformDetails.sc + '<br />';
        detailsContent += 'Lat, Lon: ' + platformDetails.lat;
        detailsContent += ', ' + platformDetails.lng + '<br />';
        detailsDiv.html(detailsContent);
        detailsDiv.find('.ui-dialog-titlebar-close').click(closeDetailsFn);
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

            detailsContent = buttonElement + '<br />';
            detailsContent += platformDetails.sc + '<br />';
            if (additionalDetails.tnm)
              detailsContent += additionalDetails.tnm + '<br />';
            detailsContent += 'Lat, Lon: ' + platformDetails.lat;
            detailsContent += ', ' + platformDetails.lng + '<br />';
            if (additionalDetails.tel)
              detailsContent += 'Elevation: ' + additionalDetails.tel;
            detailsDiv.html(detailsContent);
            detailsDiv.find('.ui-dialog-titlebar-close').click(closeDetailsFn);
            obsTable.find('tbody > tr').remove();
            if (thisLayer._hasObs())
            {
              var obsList = additionalDetails.obs;
              if (!obsList || obsList.length === 0)
              {
                obsTable.find('tbody:last-child').append('<tr><td colspan="' + colCount + '">No data</td></tr>');
              }
              else
              {

                obsList.sort(function (a, b)
                {
                  var comp = a.ot.localeCompare(b.ot);
                  if (comp !== 0)
                    return comp;
                  else
                    return a.si * 1 - b.si * 1;
                });

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

                  if (!thisLayer.isForecastOnly())
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

                  if (!thisLayer.isForecastOnly())
                  {
                    newRows += "<td class=\"conf\">" + iObs.cv + "%</td>\n";
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
                      newRows += "    <td class='td-qch'><img alt='Icon' src=\"image/";
                      newRows += oFlag.im;
                      newRows += ".png\" alt=\"";
                      newRows += oFlag.tx;
                      newRows += "\"/></td>\n";
                    }
                  }

                  newRows += '</tr>';
                }

                obsTable.find('tbody:last-child').append(newRows);
              }
            }
            else
            {
              var sensorList = additionalDetails.sl;
              if (!sensorList || sensorList.length === 0)
              {
                obsTable.find('tbody:last-child').append('<tr><td colspan="' + colCount + '">No data</td></tr>');
              }
              else
              {
                var newRows = '';
                for (var rowIndex = 0; rowIndex < sensorList.length; ++rowIndex)
                {

                  var iSensor = sensorList[rowIndex];
                  newRows += '<tr>\n';
                  newRows += '<td class="obsType">' + iSensor.ot + '</td>\n';
                  newRows += '<td class="sensorIndex">' + iSensor.idx + '</td>\n';
                  newRows += '<td class="sensorMake">' + iSensor.mfr + '</td>\n';
                  newRows += '<td class="sensorModel">' + iSensor.model + '</td>\n';
                  newRows += '</tr>\n';
                }
                obsTable.find('tbody:last-child').append(newRows);
              }
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
      var currentTime = this._wxdeMap.getSelectedTime();
      var requestData = false;
      var highestValidZoomIndex = -1;

      //check if this layer is enabled/selected and make sure that the
      //layer is added to or removed from the map based on whether the
      //layer is enabled at the current zoom/time selection
      if (this.isEnabled(currentZoom, currentTime))
      {
        if (this._checkbox)
          this._checkbox.disabled = false;
        if (this.isUserSelected())
        {
          if (!this._wxdeMap.hasLayer(this))
            this._wxdeMap.addLayer(this);
        }
        else
          return;
      }
      else
      {
        if (this._wxdeMap.hasLayer(this))
          this._wxdeMap.removeLayer(this);
        if (this._checkbox)
          this._checkbox.disabled = true;

        return;
      }


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
              var newLayers = thisLayer._layerParser(zoomLayers[zoomLevel], thisLayer._hasObs());
              var zoomLayer = thisLayer._getZoomLayer(zoomLevel);
              for (var layerIndex = 0; layerIndex < newLayers.length; ++layerIndex)
              {
                var layer = newLayers[layerIndex];

                thisLayer.layerStyler.styleLayer(layer);
                zoomLayer.addLayer(layer);
                layer.requestBounds = bounds;
                if (thisLayer.showObsLabels())
                {
                  var value;
                  if (thisLayer._wxdeMap.hasStationCodeLabels)
                    value = layer.getStationCode();
                  else if (thisLayer._wxdeMap.useMetricValue())
                    value = layer.metricValue;
                  else
                    value = layer.englishValue;
                  if (value)
                  {
                    var obsMarker = L.wxdeObsMarker(layer.getLatLng(), value);
                    layer.obsMarker = obsMarker;
                    zoomLayer.addLayer(obsMarker);
                  }
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
L.wxdeLayer = function (baseUrl, minZoom, layerParser, layerStyler, options)
{
  return new L.WxdeLayer(baseUrl, minZoom, layerParser, layerStyler, options);
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
  initialize: function (id, lat, lng, stationCode, options)
  {
    L.CircleMarker.prototype.initialize.call(this, new L.LatLng(lat, lng), options);
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
L.wxdeCircleMarker = function (id, lat, lng, stationCode, options)
{
  return new L.WxdeCircleMarker(id, lat, lng, stationCode, options);
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
L.wxdeSquareMarker = function (id, lat, lng, stationCode, options)
{
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
    stationCodeDiv: null,
    status: "0"
  },
  initialize: function (id, latlngs, midLatLng, stationCode, options)
  {

    this.midLatLng = midLatLng;

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
L.wxdePolyline = function (id, latlngs, midLatLng, stationCode, options)
{
  return new L.WxdePolyline(id, latlngs, midLatLng, stationCode, options);
};



