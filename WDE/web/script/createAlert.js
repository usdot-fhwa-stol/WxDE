{

  var selectedRectangle, nw, se;
  $(document).ready(function ()
  {
    var editId = $(document).getUrlParam("editId");

    var reportStepDivs = $("#divReportStep1, #divReportStep2, #divReportStep3");


    function setReportStep(step)
    {
      reportStepDivs.hide();
      $("#divReportStep" + step).show();
    }


    function buildSelect(items)
    {
      var rtn = '<select>';
      for (var i = 0; i < items.length; ++i)
      {
        var item = items[i];
        var label, value;
        if (item.value)
        {
          value = item.value;
          label = item.label;
        }
        else
          label = value = item;

        rtn += '<option value="' + value + '">' + label + '</option>';
      }
      rtn += '</select>';
      return rtn;
    }

    var obsList;

    var btnClickAdd;
    var btnClickRemove;
    function addRow(addHandler = true)
    {
      var newRow = $('<tr>' +
              '<td class="alert-filter">' + buildSelect(['Any', 'Mean', 'Mode']) + '</td>' +
              '<td class="alert-obstype">' + buildSelect(obsList) + '</td>' +
              '<td class="alert-operator">' + buildSelect([{value: 'Gt', label: '>'}, {value: 'Lt', label: '<'}, {value: 'GtEq', label: '>='}, {value: 'LtEq', label: '<='}]) + '</td>' +
              '<td class="alert-val"><input type="text" /></td>' +
              '<td class="alert-tol"><input type="text" /></td>' +
              '<td class="alert-add-remove"><input type="button" value="+" /></td>' +
              '</tr>');

      newRow.prependTo('#alert-conditions tbody');

      if (addHandler)
        newRow.find('input[type="button"]').on('click', btnClickAdd);
      return newRow;
    }

    btnClickRemove = function ()
    {
      $(this).parents('tr').remove();
    };

    btnClickAdd = function ()
    {
      $('#alert-conditions input[type="button"]').off('click', btnClickAdd).on('click', btnClickRemove).val('-');
      addRow();
    };


    $.ajax({
      type: "GET",
      url: "/ObsType/list",
      complete: function (data, status)
      {
        obsList = $.parseJSON(data.responseText);
        $.each(obsList, function (index, value)
        {
          var unit = value.englishUnits;
          value.label = (unit) ? value.name + ' (' + unit + ')' : value.name;
          value.value = value.id;
        });

        if (editId)
          loadNotification(editId);
        else
          addRow();
      },
      timeout: 3000
    });

    function getHighlightStyle(style)
    {
      var highlightStyle = {};
      for (var styleAttr in style)
      {
        if (style.hasOwnProperty(styleAttr))
          highlightStyle[styleAttr] = style[styleAttr];
      }
      highlightStyle.color = "#CFF";

      return highlightStyle;
    }


    function setStandardStyleProperties(style)
    {
      style.radius = 4;
      style.fillColor = style.color;
      style.color = 'black';
      style.weight = 1;
      style.opacity = 1;
      style.fillOpacity = 1;
      return style;
    }


    function setStandardPolylineStyleProperties(style)
    {
      style.weight = 5;
      style.opacity = 1;
      style.fillOpacity = 1;
      return style;
    }


    var tileLayer = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      /**
       var tileLayer = L.tileLayer('http://localhost:8090/tiles/mapbox.light/{z}/{x}/{y}.png', {
       var tileLayer = L.tileLayer('http://otile{s}.mqcdn.com/tiles/1.0.0/map/{z}/{x}/{y}.png', {
       */



      maxZoom: 17,
//  subdomains: ['1', '2', '3', '4'] ,
      subdomains: ['a', 'b', 'c'],
      attribution: '',
      id: 'mapbox.streets'
    });

    $.ajax({
      type: "GET",
      url: "/ResetSession"
    });



    var queryTime = new Date().getTime();
    map = L.wxdeSummaryMap('map_canvas', {
      center: [35, -97],
      attributionControl: false,
      zoom: 3,
      layers: [tileLayer],
      selectedTimeFunction: function ()
      {
        return queryTime;
      },
      selectedObsTypeFunction: function ()
      {
        return "0";
      }
    });

    var metaDataStyle = setStandardStyleProperties({color: "grey"});
    metaDataStyle.radius = 8;
    var metaHighlightStyle = getHighlightStyle(metaDataStyle);
    var metaDataOptions = {hasObs: false, checkbox: document.getElementById("chkMetaDataLayer"), highlighter: new StaticLayerStyler(metaHighlightStyle)};
    map.registerWxdeLayer(L.wxdeLayer('/MetaDataLayer', createCircleMarkers, new StaticLayerStyler(metaDataStyle), metaDataOptions));


    var highlightRoadStyle = setStandardPolylineStyleProperties({color: '#CFF'});
    highlightRoadStyle.weight = 16;
    var roadOptions = {checkbox: document.getElementById("chkRoadLayer"),
      highlighter: new RoadHighlighter(highlightRoadStyle, map),
      showObsLabels: false,
      isForecastOnly: true,
      enabledForTime: function (time)
      {
        var now = new Date();

        //  now.setTime(now.getTime() + (now.getTimezoneOffset() * 60 * 1000));
        now.setMinutes(0);
        now.setSeconds(0);
        now.setMilliseconds(0);


        return time > now.getTime();
      },
      obsRequestBoundsFunction: function (layer)
      {
        return L.latLngBounds(layer.getLatLng(), layer.getLatLng());
      }};

    var roadStyles = [];
    roadStyles["0"] = setStandardPolylineStyleProperties({color: "#F494FE"});
    roadStyles["1"] = setStandardPolylineStyleProperties({color: "#DA03F1"});
    roadStyles["2"] = setStandardPolylineStyleProperties({color: "#71017D"});

    var roadStyler = new RoadStatusStyler(roadStyles, map, roadOptions.highlighter);

    map.registerWxdeLayer(L.wxdeLayer('/RoadLayer', processPolylineData, roadStyler, roadOptions));

    function setupMarkers(rectangle)
    {
      var bounds = rectangle.getBounds();
      nw = L.marker(bounds.getNorthWest(), {draggable: true}).addTo(map);
      se = L.marker(bounds.getSouthEast(), {draggable: true}).addTo(map);

      var dragEnd = function (e3)
      {
        rectangle.setBounds([nw.getLatLng(), se.getLatLng()]);
      };

      nw.on("dragend", dragEnd);
      se.on("dragend", dragEnd);

    }

    var mouseDown = function (e)
    {
      selectedRectangle = L.rectangle([e.latlng, e.latlng]);
      selectedRectangle.addTo(map);

      var mouseMove = function (e2)
      {
        selectedRectangle.setBounds([e.latlng, e2.latlng]);
      };

      map.on('mousemove', mouseMove);
      var mouseOut;
      mouseOut = function ()
      {
        map.off('mousemove', mouseMove);
        map.off('mousedown', mouseDown);
        map.off('mouseup', mouseOut);
        map.off('mouseout', mouseOut);

        map.dragging.enable();

        document.getSelection().removeAllRanges();

        setReportStep(3);

        setupMarkers(selectedRectangle);
      };

      map.on('mouseup', mouseOut);
      map.on('mouseout', mouseOut);

    };



    $("#btnInitReport").click(function ()
    {
      map.on('mousedown', mouseDown);
      map.dragging.disable();
      setReportStep(2);
    });


    $("#btnResetArea").click(function ()
    {
      map.removeLayer(selectedRectangle);
      map.removeLayer(se);
      map.removeLayer(nw);
      setReportStep(1);
    });

    $("#btnSave").click(function ()
    {

      var notification = {};
      notification.conditions = [];
      notification.message = $("#txtNotificationMessage").val();
      var selectedBounds = selectedRectangle.getBounds();

      var selectedNw = selectedBounds.getNorthWest();
      var selectedSe = selectedBounds.getSouthEast();
      notification.lat1 = selectedNw.lat.toFixed(6);
      notification.lon1 = selectedNw.lng.toFixed(6);
      notification.lat2 = selectedSe.lat.toFixed(6);
      notification.lon2 = selectedSe.lng.toFixed(6);

      $("#alert-conditions tbody tr").each(function ()
      {
        var jqThis = $(this);
        var condition = {};
        notification.conditions.push(condition);
        condition.filter = jqThis.find("TD.alert-filter").children().val();
        condition.obstypeId = jqThis.find("TD.alert-obstype").children().val();
        condition.operator = jqThis.find("TD.alert-operator").children().val();
        condition.tolerance = jqThis.find("TD.alert-tol").children().val();
        condition.value = jqThis.find("TD.alert-val").children().val();
      });
      /*
       *  '<td class="alert-filter">' + buildSelect(['any', 'mean', 'mode']) + '</td>' +
       '<td class="alert-obstype">' + buildSelect(obsList) + '</td>' +
       '<td class="alert-operator">' + buildSelect(['>', '<', '>=', '<=']) + '</td>' +
       '<td class="alert-val"><input type="text" /></td>' +
       '<td class="alert-tol"><input type="text" /></td>' +
       */
      /*
       *
       "conditions": [{
       "filter": "Mode",
       "obstypeId": "0",
       "operator": "Gt",
       "tolerance": "0.8",
       "value": "0.0"
       }],
       "lat1": "0.5",
       "lat2": "0.1234",
       "lon1": "1.6",
       "lon2": "111110.46234",
       "message": "aoeu"
       }

       */
      //?' + csrf_nonce_param

      var url = '/resources/notifications';
      var method;
      if (editId)
      {
        url += '/' + editId;
        method = 'PUT';
      }
      else
        method = 'POST';

      $.ajax({
        type: method,
        url: url + '?' + csrf_nonce_param,
        data: JSON.stringify(notification),
        success: function (data)
        {
          window.location.href = '/auth2/viewAlerts.jsp?' + csrf_nonce_param;
        },
        contentType: "application/json",
        dataType: 'json'
      });
    });


    function loadNotification(id)
    {
      $.getJSON('/resources/notifications/' + id + '?' + csrf_nonce_param,
              function (notification, status)
              {
                selectedRectangle = L.rectangle([[notification.lat1, notification.lon1], [notification.lat2, notification.lon2]]);
                selectedRectangle.addTo(map);

                setupMarkers(selectedRectangle);

                setReportStep(3);

                map.fitBounds(selectedRectangle.getBounds());
                $('#txtNotificationMessage').val(notification.message);

                $.each(notification.conditions, function (index, condition)
                {
                  var conditionRow = addRow(false);

                  conditionRow.find("TD.alert-filter").children().val(condition.filter);
                  conditionRow.find("TD.alert-obstype").children().val(condition.obstypeId);
                  conditionRow.find("TD.alert-operator").children().val(condition.operator);
                  conditionRow.find("TD.alert-tol").children().val(condition.tolerance);
                  conditionRow.find("TD.alert-val").children().val(condition.value);
                });

                var conditionList = $('#alert-conditions input[type="button"]');

                conditionList.slice(1).on('click', btnClickRemove).val('-');
                conditionList.first().on('click', btnClickAdd).val('+');
              });
    }

    $.getJSON('/states/status', function (response)
    {


      function onEachFeature(feature, layer)
      {
        layer.on({
          mouseover: highlightFeature,
          mouseout: resetHighlight,
          click: resetHighlight
        });
        function highlightFeature(e)
        {
          var layer = e.target;
          layer.setStyle({
            weight: 5,
            dashArray: '',
            opacity: 0.8,
            fillOpacity: 0.8
          });
          if (!L.Browser.ie && !L.Browser.opera)
          {
            layer.bringToFront();
          }
        }

        function resetHighlight(e)
        {
          statesGroup.resetStyle(e.target);
        }
      }


      var statusStyles = [];
      statusStyles["1"] = {
        fillColor: '#FF8822',
        weight: 2,
        opacity: 0.5,
        fillOpacity: 0.5,
        color: 'white',
        dashArray: '3'
      };

      statusStyles["2"] = {
        fillColor: '#7722FF',
        weight: 2,
        opacity: 0.5,
        fillOpacity: 0.5,
        color: 'white',
        dashArray: '3'
      };

      statusStyles["3"] = {
        fillColor: '#555555',
        opacity: 0.5,
        fillOpacity: 0.5,
        color: 'white',
        dashArray: '3'
      };

      var style = {
        fillColor: 'green',
        weight: 2,
        opacity: 0.5,
        fillOpacity: 0.5,
        color: 'white',
        dashArray: '3'
      };

      var styleState = function (feature)
      {

        var stateStatus = feature.properties.status;
        if (!stateStatus)
          return style;
        if (stateStatus)
          return statusStyles[stateStatus];
        else
          return style;
      };



      var stateStatuses = response;
      var statesData = {};
      statesData.type = "FeatureCollection";
      statesData.features = [];
      for (var state in statePolygonPoints)
      {
        var stateStatus = stateStatuses[state];
        if (!stateStatus)
          continue;
        var stateFeature = {};
        stateFeature.type = "Feature";
        stateFeature.properties = {};
        stateFeature.properties.code = state;
        stateFeature.properties.status = stateStatus;
        stateFeature.geometry = {};
        stateFeature.geometry.type = "Polygon";
        stateFeature.geometry.coordinates = statePolygonPoints[state];
        statesData.features.push(stateFeature);
      }


      statesGroup = L.geoJson(statesData, {style: styleState, onEachFeature: onEachFeature});

      map.setStatesLayer(statesGroup);
    });

    setReportStep(1);

  });

}