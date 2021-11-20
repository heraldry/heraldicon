(ns heraldry.coat-of-arms.field.shared
  (:require
   [heraldry.coat-of-arms.field.environment :as environment]
   [heraldry.coat-of-arms.field.interface :as ui-interface]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.math.bounding-box :as bounding-box]
   [heraldry.math.svg.path :as path]
   [heraldry.util :as util]))

(def overlap-stroke-width 0.1)

(defn field-path-allowed? [{:keys [path counterchanged-paths]}]
  (let [component-path (->> path
                            reverse
                            (drop-while (comp not int?)))
        index (first component-path)
        components-path (->> component-path
                             (drop 1)
                             reverse
                             vec)
        length (count components-path)]
    (loop [[counterchanged-path & rest] counterchanged-paths]
      (if (nil? counterchanged-path)
        true
        (let [counterchanged-path (vec counterchanged-path)
              start (when (-> counterchanged-path count (>= length))
                      (subvec counterchanged-path 0 length))]
          (if (and (= start components-path)
                   (>= index (get counterchanged-path length)))
            false
            (recur rest)))))))

(defn add-tinctures-to-mapping [context counterchange-tinctures]
  (if (-> counterchange-tinctures count (= 2))
    (let [[t1 t2] counterchange-tinctures
          tincture-replacer {t1 t2
                             t2 t1}]
      (update context
              :tincture-mapping
              (fn [tincture-mapping]
                (-> tincture-mapping
                    (->>
                     (map (fn [[k v]]
                            [k (get tincture-replacer v v)]))
                     (into {}))
                    (as-> new-mapping
                      (cond-> new-mapping
                        (not (contains? new-mapping t1)) (assoc t1 t2)
                        (not (contains? new-mapping t2)) (assoc t2 t1)))))))
    context))

(defn render-components [context]
  [:<>
   (doall
    (for [idx (range (interface/get-list-size (c/++ context :components)))
          :while (field-path-allowed?
                  (c/++ context :components idx))]
      ^{:key idx}
      [interface/render-component
       (c/++ context :components idx)]))])

(declare render)

(defn render-counterchanged-field [{:keys [path
                                           parent-field-path
                                           parent-field-environment] :as context}]
  (if parent-field-path
    (let [counterchange-tinctures (interface/get-counterchange-tinctures
                                   (c/<< context :path parent-field-path))
          context (-> context
                      (update :counterchanged-paths conj path)
                      (add-tinctures-to-mapping counterchange-tinctures))]
      [:<>
       [render (-> context
                   (c/<< :path parent-field-path)
                   (c/<< :environment parent-field-environment))]
       [render-components context]])
    [:<>]))

(defn render [{:keys [path
                      environment
                      svg-export?
                      transform] :as context}]
  (if (= (interface/get-sanitized-data (c/++ context :type)) :heraldry.field.type/counterchanged)
    (render-counterchanged-field context)
    (let [selected? false
          ;; TODO: for refs the look-up still has to be raw, maybe this can be improved, but
          ;; adding it to the choices in the option would affect the UI
          field-type (interface/get-raw-data (c/++ context :type))
          field-context (cond-> context
                          (= field-type
                             :heraldry.field.type/ref) (->
                                                        c/--
                                                        (c/++ (interface/get-raw-data
                                                               (c/++ context :index)))))]
      [:<>
       [:g {:style (when (not svg-export?)
                     {:pointer-events "visiblePainted"
                      :cursor "pointer"})
            :transform transform}
        [ui-interface/render-field field-context]
        [render-components
         (-> field-context
             (assoc :parent-field-path path)
             (assoc :parent-field-environment environment))]]
       (when selected?
         [:path {:d (:shape environment)
                 :style {:opacity 0.25}
                 :fill "url(#selected)"}])])))

(defn -make-subfields [{:keys [svg-export?] :as context} paths parts mask-overlaps parent-environment]
  [:<>
   (doall
    (for [[idx [part-context [shape-path bounding-box-points & extra] overlap-paths]]
          (->> (map vector paths parts mask-overlaps)
               (map-indexed vector))]
      (let [clip-path-id (util/id (str "clip-" idx))
            mask-id (util/id (str "mask-" idx))
            inherit-environment? (interface/get-sanitized-data
                                  (c/++ part-context :inherit-environment?))
            counterchanged? (= (interface/get-sanitized-data
                                (c/++ part-context :type))
                               :heraldry.field.type/counterchanged)
            env (environment/create
                 (path/make-path shape-path)
                 {:parent context
                  :parent-environment parent-environment
                  :bounding-box (bounding-box/bounding-box bounding-box-points)
                  :override-environment (when (or inherit-environment?
                                                  counterchanged?)
                                          parent-environment)
                  :mask (first extra)})
            environment-shape (:shape env)]
        ^{:key idx}
        [:<>
         [:defs
          [(if svg-export?
             :mask
             :clipPath) {:id clip-path-id}
           [:path {:d environment-shape
                   :fill "#fff"}]
           (when svg-export?
             (cond
               (= overlap-paths :all) [:path {:d environment-shape
                                              :fill "none"
                                              :stroke-width overlap-stroke-width
                                              :stroke "#fff"}]
               overlap-paths (doall
                              (for [[idx shape] (map-indexed vector overlap-paths)]
                                ^{:key idx}
                                [:path {:d shape
                                        :fill "none"
                                        :stroke-width overlap-stroke-width
                                        :stroke "#fff"}]))))]
          (when-let [mask-shape (-> env :meta :mask)]
            [:mask {:id mask-id}
             [:path {:d environment-shape
                     :fill "#fff"}]
             [:path {:d mask-shape
                     :fill "#000"}]])]

         [:g {(if svg-export?
                :mask
                :clip-path) (str "url(#" clip-path-id ")")}
          [:g {:mask (when (-> env :meta :mask)
                       (str "url(#" mask-id ")"))}
           [render (c/<< part-context :environment env)]]]])))])

(defn make-subfields [context parts mask-overlaps parent-environment]
  (-make-subfields context
                   (map (fn [idx]
                          (c/++ context :fields idx)) (-> parts count range))
                   parts mask-overlaps parent-environment))

(defn make-subfield [context part mask-overlap]
  (-make-subfields (c/-- context)
                   [context] [part] [mask-overlap] (:environment context)))
