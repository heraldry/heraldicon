(ns heraldicon.frontend.library.collection.details
  (:require
   [cljs.core.async :refer [go]]
   [heraldicon.context :as c]
   [heraldicon.font :as font]
   [heraldicon.frontend.attribution :as attribution]
   [heraldicon.frontend.component.element :as component.element]
   [heraldicon.frontend.component.entity.collection.element :as element]
   [heraldicon.frontend.component.form :as form]
   [heraldicon.frontend.component.tree :as tree]
   [heraldicon.frontend.context :as context]
   [heraldicon.frontend.entity.buttons :as buttons]
   [heraldicon.frontend.entity.details :as details]
   [heraldicon.frontend.history.core :as history]
   [heraldicon.frontend.js-event :as js-event]
   [heraldicon.frontend.layout :as layout]
   [heraldicon.frontend.library.collection.list :as library.collection.list]
   [heraldicon.frontend.library.collection.shared :refer [entity-type]]
   [heraldicon.frontend.message :as message]
   [heraldicon.frontend.repository.entity-for-rendering :as entity-for-rendering]
   [heraldicon.frontend.title :as title]
   [heraldicon.heraldry.default :as default]
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bb]
   [re-frame.core :as rf]))

(defn- render-add-arms [x y size]
  (let [r (* size 0.4)
        bar-width (* size 0.5)
        bar-height (* size 0.1)]
    [:g {:transform (str "translate(" x "," y ")")}
     [:circle
      {:r r
       :style {:fill "#ccc"
               :stroke-width 0.1
               :stroke "#000"}}]
     [:rect
      {:x (/ bar-width -2)
       :y (/ bar-height -2)
       :width bar-width
       :height bar-height
       :style {:fill "#fff"}}]
     [:rect
      {:x (/ bar-height -2)
       :y (/ bar-width -2)
       :width bar-height
       :height bar-width
       :style {:fill "#fff"}}]]))

(defn- selected-element-index [form-db-path]
  (let [selected-node-path @(rf/subscribe [::element/highlighted-element])
        index (last selected-node-path)]
    (when (int? index)
      (if (< index @(rf/subscribe [:get-list-size (conj form-db-path :data :elements)]))
        index
        ;; index not valid anymore
        (do
          (rf/dispatch [::element/highlight nil])
          nil)))))

(defn- arms-highlight [path x y width height]
  (if @(rf/subscribe [::element/highlighted? path])
    [:rect {:x (- x (/ width 2) 7)
            :y (- y (/ height 2) 7)
            :rx 10
            :width (+ width 14)
            :height (+ height 14)
            :fill "#33f8"}]
    [:<>]))

(defn- arms-context [form-db-path path]
  (let [{arms-id :id
         version :version} @(rf/subscribe [:get (conj path :reference)])
        {:keys [status]
         data-path :path} (when arms-id
                            @(rf/subscribe [::entity-for-rendering/data arms-id version]))
        entity-path (when data-path
                      (conj data-path :entity))
        loaded? (= status :done)]
    {:context (-> context/default
                  (c/<< :path (if loaded?
                                (conj entity-path :data :achievement :coat-of-arms)
                                [:ui :empty-coat-of-arms]))
                  (c/<< :render-options-path (when loaded?
                                               (conj entity-path :data :achievement :render-options)))
                  (c/<< :override-render-options-path (conj form-db-path :data :render-options)))
     :entity-path entity-path}))

(defn- render-arms [form-db-path x y path & {:keys [font font-size]
                                             :or {font-size 12}}]
  (let [arms-name @(rf/subscribe [:get (conj path :name)])
        {:keys [context]} (arms-context form-db-path path)
        bounding-box (interface/get-bounding-box context)
        [width height] (bb/size bounding-box)]
    [:g
     [:title arms-name]
     [arms-highlight path x y width height]
     [:g {:transform (str "translate(" (- x (/ width 2)) "," (- y (/ height 2)) ")")}
      [interface/render-component context]
      [:text.arms-title {:x (/ width 2)
                         :y (+ height 10 font-size)
                         :text-anchor "middle"
                         :style {:font-family font
                                 :font-size font-size}}
       arms-name]]]))

(defn- render-collection [form-db-path & {:keys [allow-adding?]}]
  (let [font (some-> (interface/get-sanitized-data {:path (conj form-db-path :data :font)})
                     font/css-string)
        num-columns (interface/get-sanitized-data {:path (conj form-db-path :data :num-columns)})
        num-elements (interface/get-list-size {:path (conj form-db-path :data :elements)})
        name @(rf/subscribe [:get (conj form-db-path :name)])
        num-rows (inc (quot num-elements
                            num-columns))
        margin 10
        arms-width 100
        roll-width (+ (* num-columns
                         arms-width)
                      (* (inc num-columns)
                         margin))
        arms-height (* 1.6 arms-width)
        roll-height (+ 60
                       (* num-rows
                          arms-height)
                       (* (inc num-rows)
                          margin))]
    [:svg {:id "svg"
           :style {:width "100%"}
           :viewBox (str "0 0 " roll-width " " roll-height)
           :preserveAspectRatio "xMidYMin slice"}
     [:g
      [:text.collection-title {:x (/ roll-width 2)
                               :y 50
                               :text-anchor "middle"
                               :style {:font-family font
                                       :font-size 50}}
       name]]
     [:g {:transform "translate(0,60)"}
      (into [:<>]
            (map (fn [idx]
                   (let [x (mod idx num-columns)
                         y (quot idx num-columns)]
                     ^{:key idx}
                     [:g {:on-click (js-event/handled
                                     #(rf/dispatch [::tree/select-node (conj form-db-path :data :elements idx)]))
                          :style {:cursor "pointer"}}
                      [render-arms
                       form-db-path
                       (+ margin
                          (* x (+ arms-width
                                  margin))
                          (+ (/ arms-width 2)))
                       (+ margin
                          (* y (+ arms-height
                                  margin))
                          (+ (/ arms-height 2)))
                       (conj form-db-path :data :elements idx)
                       :font font]])))
            (range num-elements))

      (when allow-adding?
        (let [x (mod num-elements num-columns)
              y (quot num-elements num-columns)]
          ^{:key num-elements}
          [:g {:on-click (js-event/handled
                          #(rf/dispatch [::component.element/add {:path (conj form-db-path :data :elements)}
                                         default/collection-element]))
               :style {:cursor "pointer"}}
           [render-add-arms
            (+ margin
               (* x (+ arms-width
                       margin))
               (+ (/ arms-width 2)))
            (+ margin
               (* y (+ arms-height
                       margin))
               (+ (/ arms-height 2)))
            arms-width]]))]]))

(defn- render-arms-preview [form-db-path]
  (when-let [selected-element-index (selected-element-index form-db-path)]
    (let [{:keys [context
                  entity-path]} (arms-context form-db-path (conj form-db-path :data :elements selected-element-index))
          bounding-box (interface/get-bounding-box context)]
      [:<>
       (when entity-path
         [attribution/for-entity {:path entity-path}])
       [:svg {:id "svg"
              :style {:width "100%"}
              :viewBox (-> bounding-box
                           (bb/scale 5)
                           (bb/->viewbox :margin 10))
              :preserveAspectRatio "xMidYMin meet"}
        [:g {:transform "scale(5,5)"}
         [interface/render-component context]]]])))

(defn- attribution [form-db-path]
  [attribution/attribution {:path form-db-path}])

(defn- collection-form [form-db-path]
  (rf/dispatch [::title/set-from-path-or-default
                (conj form-db-path :name)
                :string.text.title/create-collection])
  (rf/dispatch-sync [::tree/node-select-default form-db-path [form-db-path]])
  (layout/three-columns
   [render-collection form-db-path :allow-adding? true]
   [:<>
    [form/active]
    [message/display entity-type]
    [buttons/buttons entity-type]
    [render-arms-preview form-db-path]
    [attribution form-db-path]]
   [:<>
    [history/buttons form-db-path]
    [tree/tree [form-db-path
                (conj form-db-path :data :render-options)
                (conj form-db-path :data)]]]
   :banner (let [entity-id @(rf/subscribe [:get (conj form-db-path :id)])
                 entity-version @(rf/subscribe [:get (conj form-db-path :version)])]
             [details/latest-version-banner
              entity-id
              entity-version
              (library.collection.list/on-select {:id entity-id})])))

(defn create-view []
  [details/create-view entity-type collection-form #(go default/collection-entity)])

(defn details-view [{{{:keys [id version]} :path} :parameters}]
  [details/by-id-view (str "collection:" id) version collection-form])
