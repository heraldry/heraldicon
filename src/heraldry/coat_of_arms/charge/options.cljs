(ns heraldry.coat-of-arms.charge.options
  (:require [heraldry.coat-of-arms.escutcheon :as escutcheon]
            [heraldry.coat-of-arms.geometry :as geometry]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.line.fimbriation :as fimbriation]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.frontend.ui.interface :as interface]))

(def default-options
  {:type {:type :choice
          ;; TODO: also a special case, probably can't include all choices here anyway
          :choices []
          :ui {:label "Type"
               :form-type :charge-type-select}}
   :origin (-> position/default-options
               (assoc :alignment nil)
               (assoc-in [:ui :label] "Origin"))
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
               (assoc-in [:ui :label] "Anchor"))
   :geometry geometry/default-options
   :escutcheon {:type :choice
                :choices (assoc-in (vec escutcheon/choices) [0 0] "Root")
                :default :none
                :ui {:label "Escutcheon"
                     :form-type :escutcheon-select}}
   :fimbriation (-> line/default-options
                    :fimbriation
                    (dissoc :alignment)
                    (assoc-in [:corner :default] :round)
                    (assoc-in [:thickness-1 :default] 10)
                    (assoc-in [:thickness-1 :max] 50)
                    (assoc-in [:thickness-1 :default] 10)
                    (assoc-in [:thickness-2 :max] 50)
                    (assoc-in [:thickness-2 :default] 10))
   :tincture {:eyes-and-teeth {:type :boolean
                               :default false
                               :ui {:label "White eyes and teeth"}}
              :shadow {:type :range
                       :min 0
                       :max 1
                       :default 1
                       :ui {:label "Shadow"
                            :step 0.01}}
              :highlight {:type :range
                          :min 0
                          :max 1
                          :default 1
                          :ui {:label "Highlight"
                               :step 0.01}}
              :ui {:label "Tinctures"
                   :form-type :tincture-modifiers}}})

(defn options [charge & {:keys [part-of-semy? part-of-charge-group?]}]
  (let [type (-> charge :type name keyword)]
    (-> (cond
          (= type :escutcheon) (options/pick default-options
                                             [[:type]
                                              [:origin]
                                              [:anchor]
                                              [:geometry]
                                              [:escutcheon]
                                              [:fimbriation]]
                                             {[:geometry :size :default] 30})
          (#{:roundel
             :annulet
             :billet
             :lozenge
             :fusil
             :mascle
             :rustre} type) (options/pick default-options
                                          [[:type]
                                           [:origin]
                                           [:anchor]
                                           [:geometry]
                                           [:fimbriation]]
                                          {[:geometry :reversed?] nil
                                           [:geometry :mirrored?] nil})
          (= type :crescent) (options/pick default-options
                                           [[:type]
                                            [:origin]
                                            [:anchor]
                                            [:geometry]
                                            [:fimbriation]]
                                           {[:geometry :mirrored?] nil})
          :else (options/pick default-options
                              [[:type]
                               [:origin]
                               [:anchor]
                               [:geometry]
                               [:fimbriation]
                               [:tincture]]))
        (cond->
         (or part-of-semy?
             part-of-charge-group?) (dissoc :origin))
        (update :origin (fn [position]
                          (when position
                            (position/adjust-options position (-> charge :origin)))))
        (update :anchor (fn [position]
                          (when position
                            (position/adjust-options position (-> charge :anchor)))))
        (update :fimbriation (fn [fimbriation]
                               (when fimbriation
                                 (-> (fimbriation/options (:fimbriation charge))
                                     (assoc :ui {:label "Fimbriation"
                                                 :form-type :fimbriation}))))))))

(defmethod interface/component-options :charge [data _path]
  (options data))
