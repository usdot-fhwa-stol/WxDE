{
  function getParameterByName(name, url)
  {
    if (!url)
      url = window.location.href;
    name = name.replace(/[\[\]]/g, "\\$&");
    var regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)"),
            results = regex.exec(url);
    if (!results)
      return null;
    if (!results[2])
      return '';
    return decodeURIComponent(results[2].replace(/\+/g, " "));
  }

  $(document).ready(function ()
  {
//
// $('<tr>' +
//              '<td class="alert-filter">' + buildSelect(['Any', 'Mean', 'Mode']) + '</td>' +
//              '<td class="alert-obstype">' + buildSelect(obsList) + '</td>' +
//              '<td class="alert-operator">' + buildSelect([{value: 'Gt', label: '>'}, {value: 'Lt', label: '<'}, {value: 'GtEq', label: '>='}, {value: 'LtEq', label: '<='}]) + '</td>' +
//              '<td class="alert-val"><input type="text" /></td>' +
//              '<td class="alert-tol"><input type="text" /></td>' +
//              '<td class="alert-add-remove"><input type="button" value="+" /></td>' +
//              '</tr>').prependTo('#alert-conditions tbody').find('input[type="button"]').on('click', btnClickAdd);
//
//    reports-table   divNotifications
    $.ajax({
      type: 'GET',
      url: '/resources/notifications?' + csrf_nonce_param,
      success: function (data)
      {
        var table = $('<table class="reports-table"><thead>' +
                '<th>message</th>' +
                '<th>bounds</th>' +
                '<th>Conditions</th><th>delete</thead></thead><tbody></tbody></table>').appendTo("#divNotifications");
        var tableBody = table.find("tbody");

        $.each(data, function (index, value)
        {

          var notificationRow =
                  $('<tr id="notification-' + value.id + '">' +
                          '<td>' + value.message + '</td>' +
                          '<td>lat1: ' + value.lat1 + '<br />lon1: ' + value.lon1 +
                          '<br />lat2: ' + value.lat2 + '<br />lon2: ' + value.lon2 + '</td>' +
                          '<td><table><thead><th>filter</th><th>obstype</th><th>comp</th><th>val</th><th>tol</th></thead><tbody></tbody></table></td><td><input type="button" value="delete" /></td></tr>');


          notificationRow.appendTo(tableBody).data('id', value.id);
          notificationRow.find('input').click(function ()
          {
            $.ajax({
              type: 'DELETE',
              url: '/resources/notifications/' + notificationRow.data('id') + '?' + csrf_nonce_param,
              success: function ()
              {
                notificationRow.remove();
              }});

          });

          var conditionTableBody = notificationRow.find('tbody');
          $.each(value.conditions, function (index, condition)
          {
            conditionTableBody.append('<tr>' +
                    '<td>' + condition.filter + '</td>' +
                    '<td>' + condition.obstypeId + '</td>' +
                    '<td>' + condition.operator + '</td>' +
                    '<td>' + condition.value + '</td>' +
                    '<td>' + condition.tolerance + '</td>' +
                    '</tr>');
          });
        });



      },
      contentType: "application/json",
      dataType: 'json'
    });


    $('#btnCreateAlert').click(function ()
    {
      window.location = '/auth2/createAlert.jsp?' + csrf_nonce_param;
    });
  });

}