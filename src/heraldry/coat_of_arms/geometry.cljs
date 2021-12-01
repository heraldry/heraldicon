(ns heraldry.coat-of-arms.geometry
  (:require
   [heraldry.gettext :refer [string]]))

(def default-options
  {:size {:type :range
          :min 5
          :max 150
          :default 50
          :ui {:label (string "Size")
               :step 0.1}}
   :stretch {:type :range
             :min 0.33
             :max 3
             :default 1
             :ui {:label (string "Stretch")
                  :step 0.01}}
   :mirrored? {:type :boolean
               :default false
               :ui {:label (string "Mirrored")}}
   :reversed? {:type :boolean
               :default false
               :ui {:label (string "Reversed")}}
   :ui {:label (string "Geometry")
        :form-type :geometry}})
