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