/* A little JQuery to shortent the typing of the alert box
correction ->  alert alert-success
attention -> alert alert-warning
danger -> alert alert-danger
info  -> alert alert-info
Also add Correction at the beginning of the div with class="correction"
*/

$(document).ready(function() {

  // $('aside').addClass('alert').addClass('alert-success');
  // $('aside').prepend('<h6><strong>Correction</strong></h6>');

  $('.attention').addClass('alert').addClass('alert-warning');
  $('.danger').addClass('alert').addClass('alert-danger');
  $('.info').addClass('alert').addClass('alert-info');


});




