# CoffeeScript utility functions.

# Toss up an in-DOM confirmation dialog. Calls onSuccess() if the user clicks
# OK. Assumes the existence of a "#modal" <div> in the page. See the main
# template.
#
# Options:
#   onFailure  - Function to call on failure, or null for none. Default: null
#   title      - Modal box title. Default: "Really?"
#   ok         - OK button text. Default: "OK"
#   cancel     - Cancel button text. Default "Cancel"
window.confirm = (msg, onSuccess, options = {}) ->
  title     = options.title || "Really?"
  onFailure = options.onFailure || undefined
  ok        = options.ok || "OK"
  cancel    = options.cancel || "Cancel"

  modal = $("#modal")
  modal.find("#modal-heading").text(title)
  okButton = modal.find("#modal-ok")
  okButton.text(ok)
  cancelButton = modal.find("#modal-cancel")
  cancelButton.text(cancel)
  modal.find("#modal-body").html(msg)

  cancelButton.unbind('click')
  cancelButton.click ->
    modal.modal('hide')
    if onFailure?
      cancelButton.click onFailure

  okButton.unbind('click')
  okButton.click (event) ->
    modal.modal('hide')
    onSuccess(event)

  modal.modal(
    backdrop: 'static'
    show:     true
  )

# Flash a message. Type can be "error" or "info". Assumes the existence of
# a <div> with id "flash" and a template with id "flash-template"
window.flash = (type, msg) ->
  elem = $("#flash-template").clone()
  elem.attr("id", "")
  elem.find(".flash-message").text(msg)
  elem.find(".alert").addClass("alert-#{type}")
  $("#flash").empty().append(elem)
  elem.show()

window.clearFlash = ->
  $("#flash").empty()

# Flag inline errors in a form by adjusting the ".help-inline" elements
# that actually have content.
window.flagInlineFormErrors = (title) ->
  $(".help-inline").each (i) ->
    text = $(this).text().trim()
    if text?.length > 0
      $(this).empty()
      $(this).append("<i class='error-icon icon-warning-sign'></i>")
      $(this).show()
      $(this).popover
        content: text
        trigger: 'hover'
        title:   "<b>#{title || "Error in form"}</b>"
