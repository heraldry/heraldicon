(ns heraldry.frontend.ui.form.render-options
  (:require [heraldry.frontend.ui.interface :as interface]
            [heraldry.render-options :as render-options]))

(defn form [path {:keys [options]}]
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
     ^{:key option} [interface/form-element (conj path option) (get options option)])])

(defmethod interface/component-node-data :heraldry.component/render-options [_path _component-data]
  {:title "Render Options"})

(defmethod interface/component-form-data :heraldry.component/render-options [component-data]
  {:form form})
