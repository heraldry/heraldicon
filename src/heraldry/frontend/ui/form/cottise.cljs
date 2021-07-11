(ns heraldry.frontend.ui.form.cottise
  (:require [heraldry.coat-of-arms.field.core :as field]
            [heraldry.frontend.ui.interface :as interface]))

(defn form [path _]
  [:<>
   (for [option [:line
                 :opposite-line
                 :distance
                 :thickness]]
     ^{:key option} [interface/form-element (conj path option)])])

(defmethod interface/component-node-data :heraldry.component/cottise [path component-data _component-options]
  {:title (str "Cottise"
               (case (last path)
                 :cottise-1 " 1"
                 :cottise-2 " 2"
                 :cottise-opposite-1 " 1 (opposite)"
                 :cottise-opposite-2 " 2 (opposite)"
                 "")
               ": "
               (-> component-data
                   :field
                   field/title))
   :nodes [{:path (conj path :field)}]})

(defmethod interface/component-form-data :heraldry.component/cottise [_path _component-data _component-options]
  {:form form})
