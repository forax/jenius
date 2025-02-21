function navbar() {
  // generate the navbar
  let navbar = $('.nav');
  let exercises = $('.exercise');
  if (exercises.length == 0) {
    navbar.remove();
    return;
  }
  exercises.each(function(index) {
    let exerciseNumber = index + 1;
    let exerciseId = `exercise${exerciseNumber}`;
    $(this).attr('id', exerciseId).addClass('tab-pane fade show');
    let itemHtml = `
        <li class="nav-item">
          <a class="nav-link" data-toggle="tab" href="#${exerciseId}">Exercice ${exerciseNumber}</a>
        </li>`;
    navbar.append(itemHtml);
  });
  exercises.eq(0).addClass('active');
}

function ttAsCode() {
  // replace all element "tt" by an element "code"
  $('.tt').each(function() {
    let code = $('<code>');
    $.each(this.attributes, function() {
      code.attr(this.name, this.value)
    });
    code.html($(this).html());
    $(this).replaceWith(code);
  });
}

$(document).ready(function() {
  navbar();
  ttAsCode();
});




