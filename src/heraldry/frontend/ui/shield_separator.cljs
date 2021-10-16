(ns heraldry.frontend.ui.shield-separator
  (:require [heraldry.coat-of-arms.default :as default]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.interface :as other-interface]))


(defmethod interface/component-node-data :heraldry.component/shield-separator [_path]
  {:title {:en "Shield layer"
           :de "Schildebene"}
   :selectable? false})

(defmethod interface/component-form-data :heraldry.component/shield-separator [_path]
  {})

(defmethod other-interface/render-component :heraldry.component/shield-separator [_path]
  [:<>])

(defn shield-separator? [element]
  (-> element
      :type
      (= :heraldry.component/shield-separator)))

(def remove-element-options
  {:post-fn (fn [elements]
              (if (->> elements
                       (filter (comp not shield-separator?))
                       count
                       zero?)
                []
                elements))})

(def add-element-options
  {:post-fn (fn [elements]
              (if (-> elements count (= 1))
                (into [default/shield-separator]
                      elements)
                elements
                ))})
