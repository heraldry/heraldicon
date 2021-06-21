(ns heraldry.frontend.ui.form.render-options
  (:require [heraldry.frontend.ui.interface :as interface]
            [heraldry.frontend.ui.option :as option]
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
     ^{:key option} [option/form (conj path option) (get options option)])])

(defmethod interface/component-node-data :heraldry.type/render-options [_path _component-data]
  {:title "Render Options"})

(defmethod interface/component-form-data :heraldry.type/render-options [component-data]
  {:form form
   :form-args {:options (render-options/options component-data)}})
