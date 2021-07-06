(ns heraldry.frontend.ui.form.render-options
  (:require [heraldry.frontend.ui.interface :as interface]))

(defn form [path _]
  [:<>
   (for [option [:escutcheon-override
                 :mode
                 :theme
                 :texture
                 :texture-displacement?
                 :shiny?
                 :escutcheon-shadow?
                 :escutcheon-outline?
                 :outline?
                 :squiggly?]]
     ^{:key option} [interface/form-element (conj path option)])])

(defmethod interface/component-node-data :heraldry.component/render-options [_path _component-data]
  {:title "Render Options"})

(defmethod interface/component-form-data :heraldry.component/render-options [_component-data]
  {:form form})
