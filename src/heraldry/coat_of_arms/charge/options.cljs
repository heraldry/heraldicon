(ns heraldry.coat-of-arms.charge.options
  (:require [heraldry.coat-of-arms.escutcheon :as escutcheon]
            [heraldry.coat-of-arms.geometry :as geometry]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.position :as position]))

(def default-options
  {:origin      (-> position/default-options
                    (assoc-in [:alignment] nil))
   :anchor      (-> position/anchor-default-options
                    (assoc-in [:point :default] :angle)
                    (update-in [:point :choices] (fn [choices]
                                                   (-> choices
                                                       drop-last
                                                       (conj (last choices))
                                                       vec)))
                    (assoc-in [:alignment] nil)
                    (assoc-in [:angle :min] -180)
                    (assoc-in [:angle :max] 180)
                    (assoc-in [:angle :default] 0))
   :geometry    geometry/default-options
   :escutcheon  {:type    :choice
                 :choices (concat [["Root" :none]]
                                  escutcheon/choices)
                 :default :none}
   :fimbriation (-> line/default-options
                    :fimbriation
                    (dissoc :alignment)
                    (assoc-in [:corner :default] :round)
                    (assoc-in [:thickness-1 :default] 10)
                    (assoc-in [:thickness-1 :max] 50)
                    (assoc-in [:thickness-1 :default] 10)
                    (assoc-in [:thickness-2 :max] 50)
                    (assoc-in [:thickness-2 :default] 10))})

(defn options [charge & {:keys [part-of-semy?]}]
  (let [type (-> charge :type name keyword)]
    (-> (cond
          (= type :escutcheon) (options/pick default-options
                                             [[:origin]
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
             :rustre} type)    (options/pick default-options
                                             [[:origin]
                                              [:anchor]
                                              [:geometry]
                                              [:fimbriation]]
                                             {[:geometry :reversed?] nil
                                              [:geometry :mirrored?] nil})
          (= type :crescent)   (options/pick default-options
                                             [[:origin]
                                              [:anchor]
                                              [:geometry]
                                              [:fimbriation]]
                                             {[:geometry :mirrored?] nil})
          :else                (options/pick default-options
                                             [[:origin]
                                              [:anchor]
                                              [:geometry]
                                              [:fimbriation]]))
        (cond->
            part-of-semy? (dissoc :origin))
        (update-in [:anchor] (fn [anchor]
                               (when anchor
                                 (position/adjust-options anchor (-> charge :anchor))))))))

