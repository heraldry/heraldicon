(ns heraldicon.export.collection
  (:require
   [heraldicon.context :as c]
   [heraldicon.entity.attribution :as attribution]
   [heraldicon.export.arms :as export.arms]
   [heraldicon.export.core :as export]
   [heraldicon.font :as font]
   [heraldicon.interface :as interface]))

(defn- collection-element [name item x y font & {:keys [preview?
                                                        font-size]
                                                 :or {font-size 12}}]
  (let [{:keys [width height]} (get item 1)]
    [:g {:transform (str "translate(" (- x (/ width 2)) "," (- y (/ height 2)) ")")}
     [:title name]
     item
     (when-not preview?
       [:text {:x (/ width 2)
               :y (+ height 10 font-size)
               :text-anchor "middle"
               :style {:font-family (font/css-string font)
                       :font-size font-size}}
        name])]))

(defn- prepare-context
  "Stores the collection data in the bound app-db and returns its context."
  [entity-data]
  (let [unique-path (export/unique-path entity-data)
        collection-data (-> (:data entity-data)
                            (assoc-in [:render-options :scope] :coat-of-arms)
                            (assoc-in [:render-options :coat-of-arms-angle] 0))]
    (export/set-data! unique-path collection-data)
    {:path unique-path}))

(defn used-fonts
  "The fonts render-svg embeds. Call it inside export/with-render-state."
  [entity-data & {:keys [add-url?]}]
  (let [context (prepare-context entity-data)]
    (cond-> [(interface/get-sanitized-data (c/++ context :font))]
      add-url? (conj export.arms/short-url-font))))

(defn render-svg
  "Renders a collection synchronously. Call it inside export/with-render-state;
  one render state for the whole document makes the ids of all arms in it
  come from one counter, so they can't collide.

  fetched-elements holds the render data of each element's arms, see
  export.arms/render-svg."
  [entity-data fetched-elements & {:keys [add-url?
                                          target-width
                                          target-height
                                          preview?
                                          embed-fonts?
                                          font-data-uris
                                          clip?]
                                   :or {embed-fonts? true}}]
  (let [context (prepare-context entity-data)
        override-render-options-path (conj (:path context) :render-options)
        num-columns (interface/get-sanitized-data (c/++ context :num-columns))
        font-title (interface/get-sanitized-data (c/++ context :font-title))
        font-scale-title (interface/get-sanitized-data (c/++ context :font-scale-title))
        font-size-title (* 50 font-scale-title)
        font (interface/get-sanitized-data (c/++ context :font))
        font-scale (interface/get-sanitized-data (c/++ context :font-scale))
        font-size-arms (* 12 font-scale)
        short-url-font export.arms/short-url-font
        used-fonts (when embed-fonts?
                     (cond-> [font]
                       add-url? (conj short-url-font)))
        elements (interface/get-raw-data (c/++ context :elements))
        [num-columns
         elements] (if preview?
                     (case (count elements)
                       1 [1 elements]
                       (2 3 4) [2 elements]
                       [3 (take 6 elements)])
                     [num-columns elements])
        num-elements (count elements)
        fetched-elements (if preview?
                           (take num-elements fetched-elements)
                           fetched-elements)
        num-rows (cond-> (quot num-elements
                               num-columns)
                   (-> num-elements
                       (rem num-columns)
                       pos?) inc)
        font-size 20
        margin 10
        arms-width 100
        roll-width (+ (* num-columns
                         arms-width)
                      (* (inc num-columns)
                         margin))
        arms-height (* (if preview?
                         1.3
                         1.6) arms-width)
        roll-height (cond-> (+ (* num-rows
                                  arms-height)
                               (* (inc num-rows)
                                  margin))
                      (not preview?) (+ 60)
                      add-url? (+ font-size margin))
        rendered-elements (mapv (fn [render-data]
                                  (export.arms/render-svg render-data
                                                          :hiccup? true
                                                          :reset-ids? false
                                                          :override-render-options-path override-render-options-path
                                                          :embed-fonts? false
                                                          :clip? clip?
                                                          :target-width arms-width))
                                fetched-elements)
        target-width (if (or target-width target-height)
                       target-width
                       1000)
        [document-width
         document-height
         document-scale] (if (or target-width
                                 target-height)
                           (let [scale-width (when target-width
                                               (/ target-width roll-width))
                                 scale-height (when target-height
                                                (/ target-height roll-height))
                                 scale (or (when (and scale-width scale-height)
                                             (min scale-width scale-height))
                                           scale-width
                                           scale-height)]
                             [(* roll-width scale)
                              (* roll-height scale)
                              scale])
                           [roll-width
                            roll-height
                            1])]
    (export/svg-markup
     [:svg {:xmlns "http://www.w3.org/2000/svg"
            :xmlnsXlink "http://www.w3.org/1999/xlink"
            :version "1.1"
            :width document-width
            :height document-height
            :viewBox (str "0 0 " (/ document-width document-scale) " " (/ document-height document-scale))}
      (when used-fonts
        [export/embed-fonts font-data-uris used-fonts])
      (when-not preview?
        [:text {:x (/ roll-width 2)
                :y 50
                :text-anchor "middle"
                :style {:font-family (font/css-string font-title)
                        :font-size font-size-title}}
         (:name entity-data)])
      (into [:<>]
            (map (fn [idx]
                   (let [x (mod idx num-columns)
                         y (quot idx num-columns)]
                     ^{:key idx}
                     [:<>
                      [collection-element
                       (:name (nth elements idx))
                       (nth rendered-elements idx)
                       (+ margin
                          (* x (+ arms-width
                                  margin))
                          (/ arms-width 2))
                       (cond-> (+ margin
                                  (* y (+ arms-height
                                          margin))
                                  (/ arms-height 2))
                         (not preview?) (+ 60))
                       font
                       :font-size font-size-arms
                       :preview? preview?]])))
            (range num-elements))
      (when add-url?
        [:text {:x margin
                :y (- roll-height
                      margin)
                :text-align "start"
                :fill "#888"
                :style {:font-family (font/css-string short-url-font)
                        :font-size font-size}}
         (attribution/full-url-for-entity-data entity-data)])])))
