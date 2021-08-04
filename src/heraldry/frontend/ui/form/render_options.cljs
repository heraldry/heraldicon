(ns heraldry.frontend.ui.form.render-options
  (:require [heraldry.frontend.ui.interface :as interface]))

(defn form [path _]
  [:<>
   (for [option [:escutcheon
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
     ^{:key option} [interface/form-element (conj path option)])])

(defmethod interface/component-node-data :heraldry.component/render-options [_path]
  {:title "Render Options"})

(defmethod interface/component-form-data :heraldry.component/render-options [_path]
  {:form form})

