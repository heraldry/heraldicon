(ns heraldicon.frontend.library.collection
  (:require
   [cljs.core.async :refer [go]]
   [heraldicon.context :as c]
   [heraldicon.font :as font]
   [heraldicon.frontend.attribution :as attribution]
   [heraldicon.frontend.entity.details :as details]
   [heraldicon.frontend.entity.details.buttons :as buttons]
   [heraldicon.frontend.history.core :as history]
   [heraldicon.frontend.layout :as layout]
   [heraldicon.frontend.library.collection.shared :refer [entity-type]]
   [heraldicon.frontend.message :as message]
   [heraldicon.frontend.repository.entity-for-rendering :as entity-for-rendering]
   [heraldicon.frontend.state :as state]
   [heraldicon.frontend.title :as title]
   [heraldicon.frontend.ui.core :as ui]
   [heraldicon.frontend.ui.form.entity.collection.element :as collection.element]
   [heraldicon.frontend.ui.shared :as shared]
   [heraldicon.heraldry.default :as default]
   [heraldicon.interface :as interface]
   [heraldicon.render.coat-of-arms :as coat-of-arms]
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
  (let [selected-node-path @(rf/subscribe [:collection-library-highlighted-element])
        index (last selected-node-path)]
    (when (int? index)
      (if (< index @(rf/subscribe [:get-list-size (conj form-db-path :data :elements)]))
        index
        ;; index not valid anymore
        (do
          (collection.element/highlight-element nil)
          nil)))))

(defn- arms-highlight [path x y width height]
  (if @(rf/subscribe [:collection-library-highlighted? path])
    [:rect {:x (- x (/ width 2) 7)
            :y (- y (/ height 2) 7)
            :rx 10
            :width (+ width 14)
            :height (+ height 14)
            :fill "#33f8"}]
    [:<>]))

(defn- render-arms [form-db-path x y size path & {:keys [font font-size]
                                                  :or {font-size 12}}]
  (let [data @(rf/subscribe [:get path])
        {arms-id :id
         version :version} (:reference data)
        {:keys [_status entity]} (when arms-id
                                   @(rf/subscribe [::entity-for-rendering/data arms-id version]))
        collection-render-options (interface/get-raw-data {:path (conj form-db-path :data :render-options)})
        {:keys [result
                environment]} (coat-of-arms/render
                               (-> shared/coa-select-option-context
                                   (c/<< :path [:context :coat-of-arms])
                                   (c/<< :render-options (merge-with
                                                          (fn [old new]
                                                            (if (nil? new)
                                                              old
                                                              new))
                                                          (:render-options shared/coa-select-option-context)
                                                          (-> entity :data :achievement :render-options)
                                                          collection-render-options))
                                   (c/<< :coat-of-arms
                                         (if-let [coat-of-arms (-> entity :data :achievement :coat-of-arms)]
                                           coat-of-arms
                                           default/coat-of-arms)))
                               size)
        {:keys [width height]} environment]
    [:g
     [:title (:name data)]
     [arms-highlight path x y width height]
     [:g {:transform (str "translate(" (- x (/ width 2)) "," (- y (/ height 2)) ")")}
      result
      [:text {:x (/ width 2)
              :y (+ height 10 font-size)
              :text-anchor "middle"
              :style {:font-family font
                      :font-size font-size}}
       (:name data)]]]))

(defn- on-arms-click [form-db-path event index]
  (state/dispatch-on-event event [:ui-component-node-select (conj form-db-path :data :elements index)]))

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
      [:text {:x (/ roll-width 2)
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
                     [:g {:on-click #(on-arms-click form-db-path % idx)
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
                       arms-width
                       (conj form-db-path :data :elements idx)
                       :font font]])))
            (range num-elements))

      (when allow-adding?
        (let [x (mod num-elements num-columns)
              y (quot num-elements num-columns)]
          ^{:key num-elements}
          [:g {:on-click #(state/dispatch-on-event
                           % [:add-element {:path (conj form-db-path :data :elements)}
                              default/collection-element])
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
    (let [arms-reference @(rf/subscribe [:get (conj form-db-path :data :elements selected-element-index :reference)])
          {arms-id :id
           version :version} arms-reference
          {:keys [status entity]} (when arms-id
                                    @(rf/subscribe [::entity-for-rendering/data arms-id version]))
          collection-render-options (interface/get-raw-data {:path (conj form-db-path :data :render-options)})]
      (when (or (not arms-id)
                (= status :done))
        (let [{:keys [result
                      environment]} (coat-of-arms/render
                                     (-> shared/coa-select-option-context
                                         (c/<< :path [:context :coat-of-arms])
                                         (c/<< :render-options (merge-with
                                                                (fn [old new]
                                                                  (if (nil? new)
                                                                    old
                                                                    new))
                                                                (:render-options shared/coa-select-option-context)
                                                                (-> entity :data :achievement :render-options)
                                                                collection-render-options))
                                         (c/<< :coat-of-arms
                                               (if-let [coat-of-arms (-> entity :data :achievement :coat-of-arms)]
                                                 coat-of-arms
                                                 default/coat-of-arms)))
                                     100)
              {:keys [width height]} environment]
          [:<>
           (when arms-id
             [:div.attribution
              [attribution/for-arms {:path [:context :arms]
                                     :arms entity}]])
           [:svg {:id "svg"
                  :style {:width "100%"}
                  :viewBox (str "0 0 " (-> width (* 5) (+ 20)) " " (-> height (* 5) (+ 20) (+ 20)))
                  :preserveAspectRatio "xMidYMin meet"}
            [:g {:transform "translate(10,10) scale(5,5)"}
             result]]])))))

(defn- collection-form [form-db-path]
  (rf/dispatch [::title/set-from-path-or-default
                (conj form-db-path :name)
                :string.text.title/create-collection])
  (rf/dispatch-sync [:ui-component-node-select-default form-db-path [form-db-path]])
  (layout/three-columns
   [render-collection form-db-path :allow-adding? true]
   [:<>
    [ui/selected-component]
    [message/display entity-type]
    [buttons/buttons entity-type]
    [render-arms-preview form-db-path]]
   [:<>
    [history/buttons form-db-path]
    [ui/component-tree [form-db-path
                        (conj form-db-path :data :render-options)
                        (conj form-db-path :data)]]]))

(defn create-view []
  [details/create-view entity-type collection-form #(go default/collection-entity)])

(defn details-view [{{{:keys [id version]} :path} :parameters}]
  [details/by-id-view (str "collection:" id) version collection-form])
