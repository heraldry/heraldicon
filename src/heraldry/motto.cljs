(ns heraldry.motto
  (:require [heraldry.coat-of-arms.geometry :as geometry]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.interface :as interface]))

(def default-options
  {:type {:type :choice
          :choices [["Motto" :heraldry.motto.type/motto]
                    ["Slogan" :heraldry.motto.type/slogan]]
          :ui {:label "Type"}}
   :origin (-> position/default-options
               (assoc-in [:ui :label] "Origin"))
   :geometry (-> geometry/default-options
                 (select-keys [:size :ui])
                 (assoc-in [:size :min] 0.1)
                 (assoc-in [:size :max] 200)
                 (assoc-in [:size :default] 100))
   :ribbon-variant {:ui {:label "Ribbon"
                         :form-type :ribbon-reference-select}}})

(defn options [data]
  default-options)

(defmethod interface/component-options :heraldry.component/motto [_path data]
  (options data))

