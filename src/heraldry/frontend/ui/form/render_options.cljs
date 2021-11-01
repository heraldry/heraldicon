(ns heraldry.frontend.ui.form.render-options
  (:require
   [heraldry.context :as c]
   [heraldry.frontend.ui.interface :as interface]))

(defn form [context]
  [:<>
   (for [option [:scope
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
                 :coat-of-arms-angle]]
     ^{:key option} [interface/form-element (c/++ context option)])])

(defmethod interface/component-node-data :heraldry.component/render-options [_context]
  {:title {:en "Render Options"
           :de "Render Optionen"}})

(defmethod interface/component-form-data :heraldry.component/render-options [_context]
  {:form form})
