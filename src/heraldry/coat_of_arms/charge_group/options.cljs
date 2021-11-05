(ns heraldry.coat-of-arms.charge-group.options
  (:require
   [heraldry.coat-of-arms.position :as position]
   [heraldry.interface :as interface]
   [heraldry.options :as options]
   [heraldry.strings :as strings]
   [heraldry.util :as util]))

(def type-choices
  [[{:en "Rows"
     :de "Zeilen"} :heraldry.charge-group.type/rows]
   [{:en "Columns"
     :de "Spalten"} :heraldry.charge-group.type/columns]
   [{:en "Arc"
     :de "Bogen"} :heraldry.charge-group.type/arc]])

(def type-map
  (util/choices->map type-choices))

(def default-options
  {:type {:type :choice
          :choices type-choices
          :ui {:label strings/type
               :form-type :charge-group-type-select}}
   :origin (-> position/default-options
               (dissoc :alignment))
   :anchor (-> position/anchor-default-options
               (assoc-in [:point :default] :angle)
               (update-in [:point :choices] (fn [choices]
                                              (-> choices
                                                  drop-last
                                                  (conj (last choices))
                                                  vec)))
               (assoc :alignment nil)
               (assoc-in [:angle :min] -180)
               (assoc-in [:angle :max] 180)
               (assoc-in [:angle :default] 0)
               (assoc-in [:ui :label] strings/anchor))
   :spacing {:type :range
             :min 1
             :max 100
             :default 40
             :ui {:label strings/spacing
                  :step 0.1}}
   :stretch {:type :range
             :min 0
             :max 5
             :default 1
             :ui {:label strings/stretch
                  :step 0.01}}
   :strip-angle {:type :range
                 :min -90
                 :max 90
                 :default 0
                 :ui {:label {:en "Strip angle"
                              :de "Streifenwinkel"}
                      :step 1}}
   :start-angle {:type :range
                 :min -180
                 :max 180
                 :default 0
                 :ui {:label {:en "Start angle"
                              :de "Startwinkel"}
                      :step 1}}
   :arc-angle {:type :range
               :min 0
               :max 360
               :default 360
               :ui {:label {:en "Arc angle"
                            :de "Bogenwinkel"}
                    :step 1}}
   :slots {:type :range
           :min 1
           :max 20
           :default 5
           :integer? true
           :ui {:label strings/number
                :form-type :charge-group-slot-number}}
   :radius {:type :range
            :min 0
            :max 100
            :default 30
            :ui {:label strings/radius
                 :step 0.1}}
   :arc-stretch {:type :range
                 :min 0
                 :max 5
                 :default 1
                 :ui {:label strings/stretch
                      :step 0.01}}
   :rotate-charges? {:type :boolean
                     :default false
                     :ui {:label {:en "Rotate charges"
                                  :de "Wappenfiguren rotieren"}}}
   :manual-blazon {:type :text
                   :default nil
                   :ui {:label strings/manual-blazon}}})

(defn options [charge-group]
  (when charge-group
    (-> (cond
          (-> charge-group :type #{:heraldry.charge-group.type/rows
                                   :heraldry.charge-group.type/columns}) (options/pick default-options
                                                                                       [[:type]
                                                                                        [:origin]
                                                                                        [:spacing]
                                                                                        [:stretch]
                                                                                        [:strip-angle]])
          (-> charge-group :type (= :heraldry.charge-group.type/arc)) (options/pick default-options
                                                                                    [[:type]
                                                                                     [:origin]
                                                                                     [:arc-stretch]
                                                                                     [:start-angle]
                                                                                     [:arc-angle]
                                                                                     [:slots]
                                                                                     [:radius]
                                                                                     [:rotate-charges?]]))
        (assoc :manual-blazon (:manual-blazon default-options))
        (update :origin (fn [position]
                          (when position
                            (-> position
                                (position/adjust-options (-> charge-group :origin))))))
        (update :anchor (fn [position]
                          (when position
                            (-> position
                                (position/adjust-options (-> charge-group :anchor)))))))))

(def strip-options
  {:slots {:type :range
           :min 0
           :max 10
           :default 3
           :integer? true
           :ui {:label strings/number
                :form-type :charge-group-slot-number}}
   :stretch {:type :range
             :min 0
             :max 5
             :default 1
             :ui {:label strings/stretch
                  :step 0.01}}
   :offset {:type :range
            :min -3
            :max 3
            :default 0
            :ui {:label strings/offset
                 :step 0.01}}})

(defmethod interface/component-options :heraldry.component/charge-group [context]
  (options (interface/get-raw-data context)))

(defmethod interface/component-options :heraldry.component/charge-group-strip [_context]
  strip-options)
