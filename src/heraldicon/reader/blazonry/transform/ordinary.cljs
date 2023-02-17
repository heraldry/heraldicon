(ns heraldicon.reader.blazonry.transform.ordinary
  (:require
   [clojure.string :as str]
   [heraldicon.heraldry.ordinary.options :as ordinary.options]
   [heraldicon.reader.blazonry.transform.cottising :refer [add-cottising]]
   [heraldicon.reader.blazonry.transform.fimbriation :refer [add-fimbriation]]
   [heraldicon.reader.blazonry.transform.line :refer [add-lines]]
   [heraldicon.reader.blazonry.transform.shared :refer [ast->hdn get-child transform-first filter-nodes]]))

(def ^:private ordinary-type-map
  (into {:ordinary/PALLET :heraldry.ordinary.type/pale
         :ordinary/BARRULET :heraldry.ordinary.type/fess
         :ordinary/CHEVRONNEL :heraldry.ordinary.type/chevron
         :ordinary/CANTON :heraldry.ordinary.type/quarter
         :ordinary/BENDLET :heraldry.ordinary.type/bend
         :ordinary/BENDLET-SINISTER :heraldry.ordinary.type/bend-sinister}
        (map (fn [[key _]]
               [(keyword "ordinary" (-> key name str/upper-case))
                key]))
        ordinary.options/ordinary-map))

(defn- get-ordinary-type [nodes]
  (let [node-type (first (get-child ordinary-type-map nodes))]
    [node-type (get ordinary-type-map node-type)]))

(def ^:private max-label-points 20)

(defn- add-ordinary-options [hdn nodes]
  (let [ordinary-options (some->> nodes
                                  (filter-nodes #{:HUMETTY
                                                  :VOIDED
                                                  :ENHANCED
                                                  :DEHANCED
                                                  :REVERSED
                                                  :THROUGHOUT
                                                  :TRUNCATED
                                                  :DOVETAILED
                                                  ;; nothing to do there, just here for reference
                                                  #_:DEXTER
                                                  :point/SINISTER
                                                  :label-points})
                                  (map (fn [[key & nodes]]
                                         [key nodes]))
                                  (into {}))
        ordinary-type (-> hdn :type name keyword)]
    (cond-> hdn
      (get ordinary-options :HUMETTY) (assoc :humetty {:humetty? true})
      (get ordinary-options :VOIDED) (assoc :voided {:voided? true})
      (get ordinary-options :REVERSED) (cond->
                                         (= :chevron
                                            ordinary-type) (assoc-in [:origin :point] :chief)
                                         (= :pall
                                            ordinary-type) (assoc-in [:origin :point] :bottom)
                                         (= :pile
                                            ordinary-type) (assoc-in [:anchor :point] :bottom))
      (get ordinary-options :THROUGHOUT) (cond->
                                           (= :pile
                                              ordinary-type) (assoc-in [:geometry :stretch] 1))
      (get ordinary-options :ENHANCED) (cond->
                                         (#{:fess
                                            :cross
                                            :saltire
                                            :pall
                                            :gore
                                            :quarter
                                            :label}
                                          ordinary-type) (assoc-in [:anchor :offset-y] 12.5)
                                         (#{:bend
                                            :bend-sinister}
                                          ordinary-type) (->
                                                           (assoc-in [:anchor :offset-y] 30)
                                                           (assoc-in [:orientation :offset-y] 30))
                                         (= :chief
                                            ordinary-type) (assoc-in [:geometry :size] (- 25 10))
                                         (= :base
                                            ordinary-type) (assoc-in [:geometry :size] (+ 25 10))
                                         (= :chevron
                                            ordinary-type) (->
                                                             (assoc-in [:anchor :offset-y] 15)
                                                             (assoc-in [:orientation :point] :angle)
                                                             (assoc-in [:orientation :angle] 45))
                                         (= :point
                                            ordinary-type) (assoc-in [:geometry :height] (- 50 25)))
      (get ordinary-options :DEHANCED) (cond->
                                         (#{:fess
                                            :cross
                                            :saltire
                                            :pall
                                            :gore
                                            :quarter
                                            :label}
                                          ordinary-type) (assoc-in [:anchor :offset-y] -12.5)
                                         (#{:bend
                                            :bend-sinister}
                                          ordinary-type) (->
                                                           (assoc-in [:anchor :offset-y] -30)
                                                           (assoc-in [:orientation :offset-y] -30))
                                         (= :chief
                                            ordinary-type) (assoc-in [:geometry :size] (+ 25 10))
                                         (= :base
                                            ordinary-type) (assoc-in [:geometry :size] (- 25 10))
                                         (= :chevron
                                            ordinary-type) (->
                                                             (assoc-in [:anchor :offset-y] -15)
                                                             (assoc-in [:orientation :point] :angle)
                                                             (assoc-in [:orientation :angle] 45))
                                         (= :point
                                            ordinary-type) (assoc-in [:geometry :height] (+ 50 25)))
      (get ordinary-options :TRUNCATED) (cond->
                                          (= :label
                                             ordinary-type) (assoc :variant :truncated))
      (get ordinary-options :DOVETAILED) (cond->
                                           (= :label
                                              ordinary-type) (assoc-in [:geometry :eccentricity] 0.4))
      (get ordinary-options :point/SINISTER) (cond->
                                               (= :gore
                                                  ordinary-type) (assoc-in [:orientation :point] :top-right)
                                               (= :point
                                                  ordinary-type) (assoc :variant :sinister)
                                               (= :quarter
                                                  ordinary-type) (assoc :variant :sinister-chief))
      (get ordinary-options :label-points) (cond->
                                             (= :label
                                                ordinary-type) (assoc :num-points
                                                                      (->> (get ordinary-options :label-points)
                                                                           (transform-first #{:amount})
                                                                           (min max-label-points)))))))

(defmethod ast->hdn :ordinary [[_ & nodes]]
  (let [[node-type ordinary-type] (get-ordinary-type nodes)]
    (-> {:type ordinary-type
         :field (transform-first #{:field} nodes)}
        (cond->
          (#{:ordinary/PALLET}
           node-type) (assoc-in [:geometry :size] 15)
          (#{:ordinary/BARRULET
             :ordinary/CHEVRONNEL
             :ordinary/BENDLET
             :ordinary/BENDLET-SINISTER}
           node-type) (assoc-in [:geometry :size] 10)
          (#{:ordinary/CANTON}
           node-type) (assoc-in [:geometry :size] 80)
          (= :heraldry.ordinary.type/gore
             ordinary-type) (assoc :line {:type :enarched
                                          :flipped? true}))
        (add-ordinary-options nodes)
        (add-lines nodes)
        (add-cottising nodes)
        (add-fimbriation nodes :line-fimbriation? (not= node-type :ordinary/LABEL)))))

(defmethod ast->hdn :ordinary-with-components [[_ & nodes]]
  (let [ordinary (transform-first #{:ordinary} nodes)
        components (transform-first #{:components} nodes)]
    [(assoc-in ordinary [:field :components] components)]))
