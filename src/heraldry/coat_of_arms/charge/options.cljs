(ns heraldry.coat-of-arms.charge.options
  (:require [heraldry.coat-of-arms.escutcheon :as escutcheon]
            [heraldry.coat-of-arms.geometry :as geometry]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.position :as position]))

(def default-options
  {:position position/default-options
   :geometry geometry/default-options
   :escutcheon {:type :choice
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

(defn options [charge]
  (let [type (-> charge :type name keyword)]
    (cond
      (= type :escutcheon) (options/pick default-options
                                         [[:position]
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
                                      [[:position]
                                       [:geometry]
                                       [:fimbriation]]
                                      {[:geometry :reversed?] nil
                                       [:geometry :mirrored?] nil})
      (= type :crescent) (options/pick default-options
                                       [[:position]
                                        [:geometry]
                                        [:fimbriation]]
                                       {[:geometry :mirrored?] nil})
      :else (options/pick default-options
                          [[:position]
                           [:geometry]
                           [:fimbriation]]))))
