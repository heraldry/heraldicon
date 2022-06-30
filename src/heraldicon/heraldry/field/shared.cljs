(ns heraldicon.heraldry.field.shared
  (:require
   [clojure.string :as s]
   [heraldicon.context :as c]
   [heraldicon.frontend.counterchange :as counterchange]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.svg.path :as path]
   [heraldicon.util.uid :as uid]))

(def ^:private overlap-stroke-width 0.1)

(defn ^:private field-path-allowed? [{:keys [path counterchanged-paths]}]
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

(defn ^:private add-tinctures-to-mapping [context counterchange-tinctures]
  (if (-> counterchange-tinctures count (= 2))
    (let [[t1 t2] counterchange-tinctures
          tincture-replacer {t1 t2
                             t2 t1}]
      (update context
              :tincture-mapping
              (fn [tincture-mapping]
                (let [new-mapping (into {}
                                        (map (fn [[k v]]
                                               [k (get tincture-replacer v v)]))
                                        tincture-mapping)]
                  (cond-> new-mapping
                    (not (contains? new-mapping t1)) (assoc t1 t2)
                    (not (contains? new-mapping t2)) (assoc t2 t1))))))
    context))

(defn ^:private render-components [context]
  (into [:<>]
        (for [idx (range (interface/get-list-size (c/++ context :components)))
              :while (field-path-allowed? (c/++ context :components idx))]
          ^{:key idx}
          [interface/render-component (c/++ context :components idx)])))

(declare render)

(defn- effective-field-context [context]
  (let [;; TODO: for refs the look-up still has to be raw, maybe this can be improved, but
        ;; adding it to the choices in the option would affect the UI
        field-type (interface/get-raw-data (c/++ context :type))]
    (cond-> context
      (= field-type
         :heraldry.field.type/ref) (->
                                     c/--
                                     (c/++ (interface/get-raw-data
                                            (c/++ context :index)))))))

(defn- render-counterchanged-field [{:keys [path
                                            parent-field-path
                                            parent-field-environment] :as context}]
  (if parent-field-path
    (let [parent-field-context (-> context
                                   (c/<< :path parent-field-path)
                                   effective-field-context)
          counterchange-tinctures (counterchange/tinctures parent-field-context)
          counterchanged-context (-> context
                                     (update :counterchanged-paths conj path)
                                     (add-tinctures-to-mapping counterchange-tinctures))]
      [:<>
       [render (-> counterchanged-context
                   (c/<< :path parent-field-path)
                   (c/<< :environment parent-field-environment)
                   effective-field-context)]
       [render-components (assoc counterchanged-context
                                 :component-of-counterchanged-field? true
                                 :previous-tincture-mapping (:tincture-mapping context))]])
    [:<>]))

(defn render [{:keys [path
                      environment
                      svg-export?
                      charge-preview?
                      transform
                      component-of-counterchanged-field?
                      previous-tincture-mapping] :as context}]
  (let [field-context (-> (effective-field-context context)
                          (dissoc :component-of-counterchanged-field?
                                  :previous-tincture-map
                                  :previous-counterchanged-paths))
        inherit-environment? (interface/get-sanitized-data
                              (c/++ field-context :inherit-environment?))
        counterchanged? (= (interface/get-raw-data (c/++ field-context :type))
                           :heraldry.field.type/counterchanged)
        field-context (cond-> field-context
                        (or inherit-environment?
                            counterchanged?) (assoc :environment
                                                    (-> environment :meta :parent-environment)))]
    (if counterchanged?
      (render-counterchanged-field field-context)
      (let [selected? false
            field-context (cond-> field-context
                            component-of-counterchanged-field? (assoc :tincture-mapping previous-tincture-mapping))]
        [:<>
         [:g {:style (when-not (or svg-export?
                                   charge-preview?)
                       {:pointer-events "visiblePainted"
                        :cursor "pointer"})
              :transform transform}
          [field.interface/render-field field-context]
          [render-components
           (assoc field-context
                  :parent-field-path path
                  :parent-field-environment environment)]]
         (when selected?
           [:path {:d (s/join "" (-> environment :shape :paths))
                   :fill-rule "evenodd"
                   :style {:opacity 0.25}
                   :fill "url(#selected)"}])]))))

(defn- make-subfields* [{:keys [svg-export?] :as context} paths parts mask-overlaps parent-environment]
  (into [:<>]
        (map-indexed (fn [idx [part-context [shape bounding-box-points meta] overlap-paths]]
                       (let [clip-path-id (uid/generate (str "clip-" idx))
                             env (environment/create
                                  (if (map? shape)
                                    (update shape :paths #(into []
                                                                (map path/make-path)
                                                                %))
                                    {:paths [(path/make-path shape)]})
                                  (merge meta
                                         {:parent context
                                          :parent-environment parent-environment
                                          :bounding-box (bb/from-points bounding-box-points)}))
                             environment-shape-paths (-> env :shape :paths)]
                         ^{:key idx}
                         [:<>
                          [:defs
                           [(if svg-export?
                              :mask
                              :clipPath) {:id clip-path-id}
                            [:path {:d (s/join "" environment-shape-paths)
                                    :clip-rule "evenodd"
                                    :fill-rule "evenodd"
                                    :fill "#fff"}]
                            (when svg-export?
                              (cond
                                (= overlap-paths :all) [:path {:d (s/join "" environment-shape-paths)
                                                               :fill "none"
                                                               :stroke-width overlap-stroke-width
                                                               :stroke "#fff"}]
                                overlap-paths (map-indexed (fn [idx shape]
                                                             ^{:key idx}
                                                             [:path {:d shape
                                                                     :fill "none"
                                                                     :stroke-width overlap-stroke-width
                                                                     :stroke "#fff"}])
                                                           overlap-paths)))]]

                          [:g {(if svg-export?
                                 :mask
                                 :clip-path) (str "url(#" clip-path-id ")")}
                           [render (c/<< part-context :environment env)]]])))
        (map vector paths parts mask-overlaps)))

(defn make-subfields [context parts mask-overlaps parent-environment]
  (make-subfields* context
                   (map (fn [idx]
                          (c/++ context :fields idx)) (-> parts count range))
                   parts mask-overlaps parent-environment))

(defn make-subfield [context part mask-overlap]
  (make-subfields* (c/-- context)
                   [context] [part] [mask-overlap] (:environment context)))
