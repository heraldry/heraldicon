(ns heraldry.motto
  (:require [heraldry.coat-of-arms.geometry :as geometry]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.interface :as interface]
            [heraldry.ribbon :as ribbon]))

(def default-options
  {:type {:type :choice
          :choices [["Motto" :heraldry.motto.type/motto]
                    ["Slogan" :heraldry.motto.type/slogan]]
          :ui {:label "Type"}}
   :origin (-> position/default-options
               (assoc-in [:point :choices] [["Top" :top]
                                            ["Bottom" :bottom]])
               (assoc-in [:point :default] :bottom)
               (dissoc :alignment)
               (assoc-in [:ui :label] "Origin"))
   :geometry (-> geometry/default-options
                 (select-keys [:size :ui])
                 (assoc-in [:size :min] 0.1)
                 (assoc-in [:size :max] 200)
                 (assoc-in [:size :default] 100))
   :ribbon-variant {:ui {:label "Ribbon"
                         :form-type :ribbon-reference-select}}
   :ribbon ribbon/default-options})

(defn options [data]
  (let [ribbon-variant? (:ribbon-variant data)
        motto-type (:type data)]
    (-> default-options
        (cond->
         ribbon-variant? (update :ribbon ribbon/options (:ribbon data))
         (not ribbon-variant?) (dissoc :ribbon)
         (= motto-type :heraldry.motto.type/slogan) (assoc-in [:origin :point :default] :top))
        (update :origin position/adjust-options))))

(defmethod interface/component-options :heraldry.component/motto [_path data]
  (options data))

