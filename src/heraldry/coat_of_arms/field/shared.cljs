(ns heraldry.coat-of-arms.field.shared
  (:require [clojure.string :as s]
            [heraldry.coat-of-arms.field.environment :as environment]
            [heraldry.coat-of-arms.field.interface :as interface]
            [heraldry.options :as options]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.util :as util]))

(def overlap-stroke-width 0.1)

(defn get-field [fields index]
  (let [part (get fields index)]
    (case (:type part)
      :heraldry.field.type/ref (get fields (:index part))
      part)))

(defn make-subfields [type fields parts mask-overlaps parent-environment parent
                      {:keys [render-field db-path svg-export?] :as context}]
  (let [mask-ids (->> (range (count fields))
                      (map (fn [idx] [(util/id (str (name type) "-" idx))
                                      (util/id (str (name type) "-" idx))])))
        environments (->> parts
                          (map-indexed (fn [idx [shape-path bounding-box & extra]]
                                         (let [field (get-field fields idx)]
                                           (environment/create
                                            (svg/make-path shape-path)
                                            {:parent parent
                                             :parent-environment parent-environment
                                             :context [type idx]
                                             :bounding-box (svg/bounding-box bounding-box)
                                             :override-environment (when (or (:inherit-environment? field)
                                                                             (:counterchanged? field))
                                                                     parent-environment)
                                             :mask (first extra)}))))
                          vec)]
    [:<>
     [:defs
      (for [[idx [clip-path-id mask-id]] (map-indexed vector mask-ids)]
        (let [env (get environments idx)
              environment-shape (:shape env)
              overlap-paths (get mask-overlaps idx)]
          ^{:key idx}
          [:<>
           [(if svg-export?
              :mask
              :clipPath) {:id clip-path-id}
            [:path {:d environment-shape
                    :fill "#fff"}]
            (cond
              (= overlap-paths :all) [:path {:d environment-shape
                                             :fill "none"
                                             :stroke-width overlap-stroke-width
                                             :stroke "#fff"}]
              overlap-paths (for [[idx shape] (map-indexed vector overlap-paths)]
                              ^{:key idx}
                              [:path {:d shape
                                      :fill "none"
                                      :stroke-width overlap-stroke-width
                                      :stroke "#fff"}]))]
           (when-let [mask-shape (-> env :meta :mask)]
             [:mask {:id mask-id}
              [:path {:d environment-shape
                      :fill "#fff"}]
              [:path {:d mask-shape
                      :fill "#000"}]])]))]

     (for [[idx [clip-path-id mask-id]] (map-indexed vector mask-ids)]
       (let [env (get environments idx)]
         ^{:key idx}
         [:g {(if svg-export?
                :mask
                :clip-path) (str "url(#" clip-path-id ")")}
          [:g {:mask (when (-> env :meta :mask)
                       (str "url(#" mask-id ")"))}
           [render-field
            (get-field fields idx)
            (get environments idx)
            (-> context
                (assoc :db-path (if (-> type
                                        name
                                        (s/split #"-" 2)
                                        first
                                        (->> (get #{"charge" "ordinary"}))) ;; FIXME: bit of a hack
                                  (conj db-path :field)
                                  (conj db-path :fields idx))))]]]))]))

(defn render [path environment
              {:keys [svg-export? transform] :as context}]
  (let [selected? false
        ;; TODO: for refs the look-up still has to be raw, maybe this can be improved, but
        ;; adding it to the choices in the option would affect the UI
        field-type (options/raw-value (conj path :type) context)
        path (if (= field-type :heraldry.field.type/ref)
               (-> path
                   drop-last
                   vec
                   (conj (options/raw-value (conj path :index) context)))
               path)]
    [:<>
     [:g {:style (when (not svg-export?)
                   {:pointer-events "visiblePainted"
                    :cursor "pointer"})
          :transform transform}
      [interface/render-field path environment context]
      #_(for [[idx element] (map-indexed vector components)]
          (let [adjusted-context (-> context
                                     (update :db-path conj :components idx))]
            ^{:key idx}
            [:<>
             (case (-> element :type namespace)
               "heraldry.ordinary.type" [ordinary/render element field environment adjusted-context]
               "heraldry.charge.type" [charge/render element field environment adjusted-context]
               "heraldry.charge-group.type" [charge-group/render element field environment adjusted-context]
               "heraldry.component" [semy/render element environment adjusted-context])]))]
     (when selected?
       [:path {:d (:shape environment)
               :style {:opacity 0.25}
               :fill "url(#selected)"}])]))

(defn make-subfields2 [field-path parts mask-overlaps parent-environment
                       {:keys [svg-export?] :as context}]
  [:<>
   (doall
    (for [[idx [shape-path bounding-box & extra]] (map-indexed vector parts)]
      (let [clip-path-id (util/id (str "clip-" idx))
            mask-id (util/id (str "mask-" idx))
            part-path (conj field-path :fields idx)
            inherit-environment? (options/sanitized-value
                                  (conj part-path :inherit-environment?)
                                  context)
            counterchanged? (options/sanitized-value
                             (conj part-path :counterchanged?)
                             context)
            env (environment/create
                 (svg/make-path shape-path)
                 {:parent field-path
                  :parent-environment parent-environment
                  :bounding-box (svg/bounding-box bounding-box)
                  :override-environment (when (or inherit-environment?
                                                  counterchanged?)
                                          parent-environment)
                  :mask (first extra)})
            environment-shape (:shape env)
            overlap-paths (get mask-overlaps idx)]
        ^{:key idx}
        [:<>
         [:defs
          [(if svg-export?
             :mask
             :clipPath) {:id clip-path-id}
           [:path {:d environment-shape
                   :fill "#fff"}]
           (cond
             (= overlap-paths :all) [:path {:d environment-shape
                                            :fill "none"
                                            :stroke-width overlap-stroke-width
                                            :stroke "#fff"}]
             overlap-paths (for [[idx shape] (map-indexed vector overlap-paths)]
                             ^{:key idx}
                             [:path {:d shape
                                     :fill "none"
                                     :stroke-width overlap-stroke-width
                                     :stroke "#fff"}]))]
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
           [render
            part-path
            env
            context]]]])))])
