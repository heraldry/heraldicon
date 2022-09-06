(ns heraldicon.frontend.library.charge.details
  (:require
   ["svgo-browser/lib/get-svgo-instance" :as getSvgoInstance]
   [cljs.core.async :refer [go]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [clojure.set :as set]
   [clojure.string :as s]
   [clojure.walk :as walk]
   [com.wsscode.async.async-cljs :refer [<? go-catch]]
   [heraldicon.context :as c]
   [heraldicon.frontend.attribution :as attribution]
   [heraldicon.frontend.component.form :as form]
   [heraldicon.frontend.component.tree :as tree]
   [heraldicon.frontend.entity.buttons :as buttons]
   [heraldicon.frontend.entity.core :as entity]
   [heraldicon.frontend.entity.details :as details]
   [heraldicon.frontend.history.core :as history]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.layout :as layout]
   [heraldicon.frontend.library.charge.shared :refer [entity-type]]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.message :as message]
   [heraldicon.frontend.modal :as modal]
   [heraldicon.frontend.shared :as shared]
   [heraldicon.frontend.svgo-setup]
   [heraldicon.frontend.title :as title]
   [heraldicon.frontend.user.session :as session]
   [heraldicon.heraldry.default :as default]
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.svg.core :as svg]
   [heraldicon.util.colour :as colour]
   [hickory.core :as hickory]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

(def ^:private example-coa-db-path
  [:example-coa])

(defn- find-colours [data]
  (->> data
       (tree-seq #(or (map? %)
                      (vector? %)
                      (seq? %)) seq)
       (filter #(and (vector? %)
                     (-> % count (= 2))
                     (#{:fill :stroke :stop-color} (first %))))
       (map second)
       (filter #(and (string? %)
                     (not= % "none")
                     (-> % (s/starts-with? "url") not)))
       (map s/lower-case)
       (map colour/normalize)
       (map s/lower-case)
       (filter #(re-matches #"^#[a-f0-9]{6}$" %))
       set))

(defn- parse-number-with-unit [s]
  (when s
    (let [[_ num unit] (re-matches #"(?i)^ *([0-9e.]*) *([a-z]*)$" s)
          value (if (-> num count (= 0))
                  0
                  (js/parseFloat num))
          factor (case (s/lower-case unit)
                   "px" 1
                   "in" 96
                   "cm" 37.795
                   "mm" 3.7795
                   "pt" 1.3333
                   "pc" 16
                   1)]
      (* value factor))))

(defn- parse-width-height-from-viewbox [s]
  (when s
    (let [[_ x0 y0 x1 y1] (re-matches #"(?i)^ *(-?[0-9e.]*) *(-?[0-9e.]*) *(-?[0-9e.]*) *(-?[0-9e.]*)$" s)
          x0 (if (-> x0 count (= 0))
               0
               (js/parseFloat x0))
          y0 (if (-> y0 count (= 0))
               0
               (js/parseFloat y0))
          x1 (if (-> x1 count (= 0))
               0
               (js/parseFloat x1))
          y1 (if (-> y1 count (= 0))
               0
               (js/parseFloat y1))]
      (if (and x0 y0 y0 y1)
        [x0 y0 x1 y1]
        [nil nil nil nil]))))

(defn- update-colours-map [charge-data]
  (let [existing-colours (:colours charge-data)
        colours (into {}
                      (map (fn [c]
                             [c :keep]))
                      (find-colours (-> charge-data :edn-data :data)))]
    (assoc charge-data
           :colours
           (merge colours
                  (select-keys existing-colours
                               (set/intersection
                                (-> colours
                                    keys
                                    set)
                                (-> existing-colours
                                    keys
                                    set)))))))

(macros/reg-event-fx ::set-svg-data
  (fn [{:keys [db]} [_ db-path edn-data raw-svg-data]]
    (let [width (parse-number-with-unit (get-in edn-data [1 :width]))
          height (parse-number-with-unit (get-in edn-data [1 :height]))
          [shift-x shift-y
           width height] (let [[viewbox-x viewbox-y
                                viewbox-width viewbox-height] (parse-width-height-from-viewbox
                                                               (or (get-in edn-data [1 :viewbox])
                                                                   (get-in edn-data [1 :viewBox])))]
                           (if (and viewbox-width viewbox-height)
                             [viewbox-x viewbox-y viewbox-width viewbox-height]
                             [0 0 width height]))
          [shift-x shift-y] (if (and shift-x shift-y)
                              [shift-x shift-y]
                              [100 100])
          [width height] (if (and width height)
                           [width height]
                           [100 100])
          prepared-edn-data (-> edn-data
                                (assoc 0 :g)
                                   ;; add fill and stroke at top level as default
                                   ;; some SVGs don't specify them for elements if
                                   ;; they are black, but for that to work we need
                                   ;; the root element to specify them
                                   ;; disadvantage: this colour will now always show
                                   ;; im the interface, even if the charge doesn't
                                   ;; contain and black elements, but they usually will
                                   ;;
                                   ;; the stroke-width is also set to 0, because areas
                                   ;; that really should not get an outline otherwise
                                   ;; would default to one of width 1
                                (assoc 1 {:fill "#000000"
                                          :stroke "none"
                                          :stroke-width 0})
                                (->> (walk/postwalk
                                      (fn [data]
                                           ;; as a follow up to above comment:
                                           ;; if we find an element that has a stroke-width but no
                                           ;; stroke, then set that stroke to none, so those thick
                                           ;; black outlines won't appear
                                           ;; TODO: this might, for some SVGs, remove some outlines,
                                           ;; namely if the element was supposed to inherit the stroke
                                           ;; from a parent element
                                        (if (and (map? data)
                                                 (contains? data :stroke-width)
                                                 (not (contains? data :stroke)))
                                          (assoc data :stroke "none")
                                          data))))
                                svg/add-ids
                                (assoc-in [1 :transform] (str "translate(" (- shift-x) "," (- shift-y) ")")))
          existing-colours (get-in db (conj db-path :colours))]
      {:db (update-in db db-path merge
                      (update-colours-map
                       {:colours existing-colours
                        :edn-data {:data prepared-edn-data
                                   :width width
                                   :height height}
                        :svg-data raw-svg-data}))
       :dispatch [::clear-selected-colours]})))

(rf/reg-fx ::process-svg-file
  (fn [[db-path raw-svg-data]]
    (go
      (try
        (let [parsed-svg-data (-> raw-svg-data
                                  (svg/optimize (fn [data]
                                                  (go-catch
                                                   (-> (clj->js {:removeUnknownsAndDefaults false
                                                                 :convertStyleToAttrs false})
                                                       getSvgoInstance
                                                       (.optimize data)
                                                       <p!))))
                                  <?
                                  hickory/parse-fragment
                                  first
                                  hickory/as-hiccup
                                  svg/fix-string-style-values
                                  svg/process-style-blocks
                                  svg/strip-unnecessary-parts
                                  svg/fix-attribute-and-tag-names
                                  svg/remove-namespaced-elements)]
          (rf/dispatch [::set-svg-data db-path parsed-svg-data raw-svg-data]))
        (catch :default e
          (log/error e "load svg file error"))))))

(macros/reg-event-fx ::load-svg-file
  (fn [_ [_ db-path data]]
    {::process-svg-file [db-path data]}))

(def ^:private show-colours-path
  [:ui :colours :show])

(macros/reg-event-db ::toggle-select-colour
  (fn [db [_ colour]]
    (update-in db (conj show-colours-path (colour/normalize colour)) not)))

(macros/reg-event-db ::clear-selected-colours
  (fn [db _]
    (assoc-in db show-colours-path nil)))

(defn- generate-new-colour [colours]
  (loop [new-colour (colour/random)]
    (if (contains? colours new-colour)
      (recur (colour/random))
      new-colour)))

(defn- colourize-element [element-id colour data]
  (walk/postwalk (fn [element]
                   (if (and (vector? element)
                            (-> element second map?)
                            (-> element second :id (= element-id)))
                     (assoc-in element [1 :fill] colour)
                     element))
                 data))

(macros/reg-event-db ::colourize-element
  (fn [db [_ form-db-path element-id]]
    (let [colours (set (keys (get-in db (conj form-db-path :data :colours))))
          new-colour (generate-new-colour colours)]
      (-> db
          (update-in (conj form-db-path :data :edn-data :data) (partial colourize-element element-id new-colour))
          (update-in (conj form-db-path :data) update-colours-map)))))

(defn- svg-fill-clicked [form-db-path element event]
  (doto event
    .preventDefault
    .stopPropagation)
  (if @(rf/subscribe [::colourize-mode?])
    (rf/dispatch [::colourize-element form-db-path (-> element second :id)])
    (rf/dispatch [::toggle-select-colour (-> element second :fill)])))

(def ^:private colourize-mode-path
  [:ui :charge-edit :colourize-mode?])

(rf/reg-sub ::colourize-mode?
  (fn [_ _]
    (rf/subscribe [:get colourize-mode-path]))

  (fn [value _]
    value))

(macros/reg-event-db ::toggle-colourize-mode
  (fn [db _]
    (update-in db colourize-mode-path not)))

(defn- edit-controls []
  (let [colourize-mode? @(rf/subscribe [::colourize-mode?])]
    [:div.no-select {:style {:position "absolute"
                             :left "20px"
                             :top "20px"}}
     [:button {:on-click #(rf/dispatch [::toggle-colourize-mode])
               :style (when colourize-mode?
                        {:color "#ffffff"
                         :background-color "#ff8020"})}
      [tr :string.button/colourize]]
     " "
     (when colourize-mode?
       [tr :string.charge.editor/colourize-info])]))

(defn- prepare-for-preview [data form-db-path]
  (update-in data
             [:data :edn-data :data]
             (fn [charge-data]
               (walk/postwalk (fn [element]
                                (if (and (vector? element)
                                         (-> element second map?)
                                         (-> element second :fill))
                                  (-> element
                                      (assoc-in [1 :on-click] (partial svg-fill-clicked form-db-path element))
                                      (assoc-in [1 :style :cursor] "pointer"))
                                  element))
                              charge-data))))

(defn- preview [form-db-path & {:keys [original?]}]
  (let [form-data @(rf/subscribe [:get form-db-path])
        prepared-charge-data (-> form-data
                                 (update :username #(or % (:username @(rf/subscribe [::session/data]))))
                                 (cond->
                                   original? (prepare-for-preview form-db-path)))
        coat-of-arms @(rf/subscribe [:get (conj example-coa-db-path :coat-of-arms)])
        context (-> shared/coa-select-option-context
                    (c/<< :path [:context :coat-of-arms])
                    (c/set-render-hint :ui-show-colours
                                       (->> @(rf/subscribe [:get show-colours-path])
                                            (keep (fn [value]
                                                    (when (second value)
                                                      (first value))))
                                            set))
                    (c/<< :render-options-path
                          (conj example-coa-db-path :render-options))
                    (c/<< :coat-of-arms
                          (assoc-in coat-of-arms
                                    [:field :components 0 :data] prepared-charge-data))
                    (c/set-render-hint :charge-preview? true)
                    (c/set-render-hint :preview-original? original?))
        bounding-box (interface/get-bounding-box context)]
    [:svg {:viewBox (-> bounding-box
                        (bb/scale 5)
                        (bb/->viewbox :margin 10))
           :preserveAspectRatio "xMidYMid meet"
           :style {:width "100%"}}
     [:g {:transform "scale(5,5)"}
      [interface/render-component context]]]))

(defn- upload-file [form-db-path event]
  (modal/start-loading)
  (let [file (-> event .-target .-files (.item 0))]
    (when file
      (let [reader (js/FileReader.)]
        (set! (.-onloadend reader) (fn []
                                     (let [raw-data (.-result reader)]
                                       (rf/dispatch [::load-svg-file (conj form-db-path :data) raw-data]))
                                     (modal/stop-loading)))
        (set! (-> event .-target .-value) "")
        (.readAsText reader file)))))

(defn- svg-buttons [form-db-path]
  (let [charge-svg-url @(rf/subscribe [:get (conj form-db-path :data :svg-data-url)])
        can-upload? (and @(rf/subscribe [::session/logged-in?])
                         (or (not @(rf/subscribe [::entity/saved? form-db-path]))
                             @(rf/subscribe [::entity/owned-by? form-db-path @(rf/subscribe [::session/data])])
                             @(rf/subscribe [::session/admin?])))]
    [:<>
     [:label.button {:for "upload"
                     :class (when-not can-upload?
                              "disabled")
                     :style {:display "inline-block"
                             :width "auto"
                             :flex "initial"
                             :margin-right "10px"}}
      [tr :string.button/upload-svg]
      [:input {:type "file"
               :accept "image/svg+xml"
               :id "upload"
               :disabled (not can-upload?)
               :on-change (when can-upload?
                            (partial upload-file form-db-path))
               :style {:display "none"}}]]
     (when charge-svg-url
       [:a {:href charge-svg-url
            :target "_blank"
            :style {:flex "initial"
                    :padding-top "0.5em"
                    :white-space "nowrap"}}
        [tr :string.miscellaneous/svg-file]])]))

(defn- attribution [form-db-path]
  (let [attribution-data (attribution/for-charge {:path form-db-path})]
    [:div.attribution
     [:h3 [tr :string.attribution/license]]
     attribution-data]))

(defn- charge-form [form-db-path]
  (rf/dispatch [::title/set-from-path-or-default
                (conj form-db-path :name)
                :string.text.title/create-charge])
  (rf/dispatch-sync [::tree/node-select-default form-db-path [form-db-path
                                                              example-coa-db-path]])
  (layout/three-columns
   [:<>
    [preview form-db-path :original? true]
    [edit-controls]]
   [:<>
    [form/active]
    [message/display entity-type]
    [buttons/buttons entity-type
     [svg-buttons form-db-path]]
    [attribution form-db-path]]
   [:<>
    [history/buttons form-db-path]
    [tree/tree [form-db-path
                :spacer
                (conj example-coa-db-path :render-options)
                :spacer
                (conj example-coa-db-path :coat-of-arms :field :components 0)]]
    [preview form-db-path]]))

(defn create-view []
  [details/create-view entity-type charge-form #(go default/charge-entity)])

(defn details-view [{{{:keys [id version]} :path} :parameters}]
  [details/by-id-view (str "charge:" id) version charge-form])
