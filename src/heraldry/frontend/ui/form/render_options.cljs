(ns heraldry.frontend.ui.form.render-options
  (:require
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.gettext :refer [string]]))

(defn form [context]
  (ui-interface/form-elements
   context
   [:scope
    :escutcheon
    :mode
    :theme
    :texture
    :texture-displacement?
    :shiny?
    :escutcheon-shadow?
    :escutcheon-outline?
    :outline?
    :squiggly?
    :coat-of-arms-angle]))

(defmethod ui-interface/component-node-data :heraldry.component/render-options [_context]
  {:title (string "Render Options")})

(defmethod ui-interface/component-form-data :heraldry.component/render-options [_context]
  {:form form})
