(ns heraldry.coat-of-arms.charge.options
  (:require [heraldry.coat-of-arms.escutcheon :as escutcheon]
            [heraldry.coat-of-arms.geometry :as geometry]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.position :as position]))

(def default-options
  {:position   position/default-options
   :geometry   geometry/default-options
   :escutcheon {:type    :choice
                :choices (concat [["Root" :none]]
                                 escutcheon/choices)
                :default :none}})

(defn options [charge]
  (when charge
    (let [type (:type charge)]
      (->
       default-options
       (options/merge
        (->
         (get {:escutcheon {:geometry {:size      {:default 30}
                                       :mirrored? nil}}
               :roundel    {:geometry {:mirrored? nil
                                       :reversed? nil}}
               :annulet    {:geometry {:mirrored? nil
                                       :reversed? nil}}
               :billet     {:geometry {:mirrored? nil
                                       :reversed? nil}}
               :lozenge    {:geometry {:mirrored? nil
                                       :reversed? nil}}
               :fusil      {:geometry {:mirrored? nil
                                       :reversed? nil}}
               :mascle     {:geometry {:mirrored? nil
                                       :reversed? nil}}
               :rustre     {:geometry {:mirrored? nil
                                       :reversed? nil}}
               :crescent   {:geometry {:mirrored? nil}}}
              type)
         (cond->
             (not= type :escutcheon) (assoc :escutcheon nil))))))))
