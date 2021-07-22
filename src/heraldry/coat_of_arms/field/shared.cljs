(ns heraldry.coat-of-arms.field.shared
  (:require [heraldry.coat-of-arms.field.environment :as environment]
            [heraldry.coat-of-arms.field.interface :as ui-interface]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.interface :as interface]
            [heraldry.util :as util]))

(def overlap-stroke-width 0.1)

(defn render [path environment
              {:keys [svg-export? transform] :as context}]
  (let [selected? false
        ;; TODO: for refs the look-up still has to be raw, maybe this can be improved, but
        ;; adding it to the choices in the option would affect the UI
        field-type (interface/get-raw-data (conj path :type) context)
        path (if (= field-type :heraldry.field.type/ref)
               (-> path
                   drop-last
                   vec
                   (conj (interface/get-raw-data (conj path :index) context)))
               path)]
    [:<>
     [:g {:style (when (not svg-export?)
                   {:pointer-events "visiblePainted"
                    :cursor "pointer"})
          :transform transform}
      [ui-interface/render-field path environment context]
      (for [idx (range (interface/get-list-size (conj path :components) context))]
        ^{:key idx}
        [interface/render-component
         (conj path :components idx)
         path environment context])]
     (when selected?
       [:path {:d (:shape environment)
               :style {:opacity 0.25}
               :fill "url(#selected)"}])]))

(defn -make-subfields [field-path paths parts mask-overlaps parent-environment
                       {:keys [svg-export?] :as context}]
  [:<>
   (doall
    (for [[idx [part-path [shape-path bounding-box & extra] overlap-paths]]
          (->> (map vector paths parts mask-overlaps)
               (map-indexed vector))]
      (let [clip-path-id (util/id (str "clip-" idx))
            mask-id (util/id (str "mask-" idx))
            inherit-environment? (interface/get-sanitized-data
                                  (conj part-path :inherit-environment?)
                                  context)
            counterchanged? (interface/get-sanitized-data
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
            environment-shape (:shape env)]
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

(defn make-subfields [field-path parts mask-overlaps parent-environment context]
  (-make-subfields field-path
                   (map (fn [idx]
                          (conj field-path :fields idx)) (-> parts count range))
                   parts mask-overlaps parent-environment context))

(defn make-subfield [part-path part mask-overlap parent-environment context]
  (-make-subfields (vec (drop-last part-path))
                   [part-path] [part] [mask-overlap] parent-environment context))
