(ns heraldry.frontend.ui.form.collection-element
  (:require [heraldry.collection.element] ;; needed for defmethods
            [heraldry.frontend.ui.interface :as interface]))

(defn form [path _]
  [:<>
   (for [option [:name
                 :reference]]
     ^{:key option} [interface/form-element (conj path option)])])

(defmethod interface/component-node-data :heraldry.component/collection-element [path component-data _component-options]
  (let [name (-> component-data :name)
        index (last path)]
    {:title (str (inc index) ": "
                 (if (-> name count pos?)
                   name
                   "<no name>"))}))

(defmethod interface/component-form-data :heraldry.component/collection-element [_path _component-data _component-options]
  {:form form})
