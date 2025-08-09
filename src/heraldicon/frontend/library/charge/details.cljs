(ns heraldicon.frontend.library.charge.details
  (:require
   [cljs.core.async :refer [go]]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [com.wsscode.async.async-cljs :refer [<? go-catch]]
   [heraldicon.context :as c]
   [heraldicon.frontend.attribution :as attribution]
   [heraldicon.frontend.canvas :as canvas]
   [heraldicon.frontend.component.form :as form]
   [heraldicon.frontend.component.tree :as tree]
   [heraldicon.frontend.context :as context]
   [heraldicon.frontend.element.charge-type-select :as-alias charge-type-select]
   [heraldicon.frontend.entity.buttons :as buttons]
   [heraldicon.frontend.entity.core :as entity]
   [heraldicon.frontend.entity.details :as details]
   [heraldicon.frontend.entity.form :as entity.form]
   [heraldicon.frontend.history.core :as history]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.layout :as layout]
   [heraldicon.frontend.library.charge.list :as library.charge.list]
   [heraldicon.frontend.library.charge.shared :refer [entity-type]]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.message :as message]
   [heraldicon.frontend.modal :as modal]
   [heraldicon.frontend.repository.charge-types :as repository.charge-types]
   [heraldicon.frontend.title :as title]
   [heraldicon.frontend.user.session :as session]
   [heraldicon.heraldry.charge.other :as other]
   [heraldicon.heraldry.default :as default]
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.vector :as v]
   [heraldicon.svg.core :as svg]
   [heraldicon.util.colour :as colour]
   [hickory.core :as hickory]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

(def ^:private preview-db-path
  [:ui :charge-preview])

(def ^:private original-preview-db-path
  [:ui :charge-original])

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
                     (-> % (str/starts-with? "url") not)))
       (map str/lower-case)
       (map colour/normalize)
       (map str/lower-case)
       (filter #(re-matches #"^#[a-f0-9]{6}$" %))
       set))

(defn- parse-number-with-unit [s]
  (when s
    (let [[_ num unit] (re-matches #"(?i)^ *([0-9e.]*) *([a-z%]*)$" s)
          value (if (-> num count (= 0))
                  0
                  (js/parseFloat num))
          factor (case (str/lower-case unit)
                   "px" 1
                   "in" 96
                   "cm" 37.795
                   "mm" 3.7795
                   "pt" 1.3333
                   "pc" 16
                   "%" 1
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

(defn- remove-invisible-colours [svg-data placeholder-colours]
  (if (empty? placeholder-colours)
    svg-data
    (-> svg-data
        (other/remove-layer-separator placeholder-colours)
        (other/remove-shading placeholder-colours))))

(defn- determine-charge-boundaries [svg-data colours]
  (go-catch
   (let [width (parse-number-with-unit (get-in svg-data [1 :width]))
         height (parse-number-with-unit (get-in svg-data [1 :height]))
         svg-data (remove-invisible-colours svg-data colours)
         [shift-x shift-y
          width height
          from-viewbox?] (let [[viewbox-x viewbox-y
                                viewbox-width viewbox-height] (parse-width-height-from-viewbox
                                                               (or (get-in svg-data [1 :viewbox])
                                                                   (get-in svg-data [1 :viewBox])))]
                           (if (and viewbox-width viewbox-height)
                             [viewbox-x viewbox-y viewbox-width viewbox-height true]
                             [0 0 width height false]))
         edn-data (assoc svg-data
                         0 :g
                         1 nil)
         naive-bounding-box (bb/from-vector-and-size
                             (v/Vector. shift-x shift-y)
                             width height)
         {:keys [bounding-box
                 shapes]} (<? (canvas/svg-shapes-and-bounding-box edn-data naive-bounding-box))]
     {:bounding-box (if (or (not from-viewbox?)
                            (< (bb/width bounding-box) width)
                            (< (bb/height bounding-box) height))
                      bounding-box
                      naive-bounding-box)
      :shapes shapes})))

(macros/reg-event-fx ::set-svg-data
  (fn [{:keys [db]} [_ db-path prepared-edn-data raw-svg-data bounding-box shapes]]
    (let [width (bb/width bounding-box)
          height (bb/height bounding-box)
          existing-colours (get-in db (conj db-path :colours))]
      (modal/stop-loading)
      {:db (update-in db db-path merge
                      (update-colours-map
                       {:colours existing-colours
                        :edn-data {:data prepared-edn-data
                                   :width width
                                   :height height
                                   :shapes shapes}
                        :svg-data raw-svg-data}))
       :dispatch [::clear-selected-colours]})))

(rf/reg-fx ::process-svg-file
  (fn [[db-path raw-svg-data colours]]
    (go
      (try
        (let [parsed-svg-data (-> raw-svg-data
                                  (svg/optimize (fn [data]
                                                  (go-catch
                                                   (js/SVGO.optimize
                                                    data
                                                    (clj->js
                                                     {:plugins [{:name "preset-default"
                                                                 :params {:overrides {:removeUnknownsAndDefaults false
                                                                                      :removeHiddenElems false
                                                                                      :convertPathData false
                                                                                      :mergePaths false}}}
                                                                "removeScripts"]})))))
                                  <?
                                  hickory/parse-fragment
                                  first
                                  hickory/as-hiccup
                                  svg/fix-string-style-values
                                  svg/process-style-blocks
                                  svg/strip-unnecessary-parts
                                  svg/fix-attribute-and-tag-names
                                  svg/remove-namespaced-elements)
              {:keys [bounding-box
                      shapes]} (<? (determine-charge-boundaries parsed-svg-data colours))
              {shift-x :x
               shift-y :y} (bb/top-left bounding-box)
              prepared-edn-data (-> parsed-svg-data
                                    (assoc 0 :g
                                           1 nil)
                                    svg/fix-stroke-and-fill
                                    (assoc-in [1 :transform]
                                              (str "translate(" (- shift-x) "," (- shift-y) ")"))
                                    svg/add-ids)]
          (rf/dispatch [::set-svg-data db-path prepared-edn-data raw-svg-data bounding-box shapes]))
        (catch :default e
          (modal/stop-loading)
          (log/error e "load svg file error"))))))

(rf/reg-event-fx ::reprocess-svg-file
  (fn [{:keys [db]} [_ db-path]]
    (modal/start-loading)
    {::process-svg-file [db-path (get-in db (conj db-path :svg-data)) (get-in db (conj db-path :colours))]}))

(macros/reg-event-fx ::load-svg-file
  (fn [{:keys [db]} [_ db-path data]]
    {::process-svg-file [db-path data (get-in db (conj db-path :colours))]}))

(def show-colours-path
  [:ui :colours :show])

(macros/reg-event-db ::toggle-select-colour
  (fn [db [_ colour]]
    (cond-> db
      (not= colour "none") (update-in (conj show-colours-path (colour/normalize colour)) not))))

(macros/reg-event-db ::clear-selected-colours
  (fn [db _]
    (assoc-in db show-colours-path nil)))

(rf/reg-sub ::selected-colours
  (fn [_ _]
    (rf/subscribe [:get show-colours-path]))

  (fn [colours _]
    (set (keep (fn [[k v]]
                 (when v
                   k))
               colours))))

(defn- generate-new-colour [colours]
  (loop [new-colour (colour/random)]
    (if (contains? colours new-colour)
      (recur (colour/random))
      new-colour)))

(defn- colourize-element [element-id colour data]
  (if element-id
    (walk/postwalk (fn [element]
                     (if (and (vector? element)
                              (-> element second map?)
                              (-> element second :id (= element-id)))
                       (assoc-in element [1 :fill] colour)
                       element))
                   data)
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

(rf/reg-sub ::original-charge-data
  (fn [_ _]
    (rf/subscribe [:get (entity.form/data-path :heraldicon.entity.type/charge)]))

  (fn [data _]
    (prepare-for-preview data (entity.form/data-path :heraldicon.entity.type/charge))))

(defn- preview [& {:keys [original?]}]
  (let [base-path (if original?
                    original-preview-db-path
                    preview-db-path)
        context (-> context/default
                    (c/<< :path (conj base-path :coat-of-arms))
                    (c/<< :render-options-path (conj base-path :render-options))
                    (c/set-render-hint :ui-show-colours @(rf/subscribe [::selected-colours])
                                       :charge-preview? true
                                       :preview-original? original?))
        bounding-box (interface/get-bounding-box context)]
    [:svg {:viewBox (-> bounding-box
                        (bb/scale 5)
                        (bb/->viewbox :margin 10))
           :preserveAspectRatio "xMidYMid meet"
           :style {:width "100%"}}
     [:g {:transform "scale(5,5)"}
      [interface/render-component context]]]))

(defn- upload-file [form-db-path event]
  (modal/start-loading-sync)
  (let [file (-> event .-target .-files (.item 0))]
    (when file
      (let [reader (js/FileReader.)]
        (set! (.-onloadend reader) (fn []
                                     (let [raw-data (.-result reader)]
                                       (rf/dispatch [::load-svg-file (conj form-db-path :data) raw-data]))))
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

(defn- charge-form [form-db-path]
  (rf/dispatch [::title/set-from-path-or-default
                (conj form-db-path :name)
                :string.text.title/create-charge])
  (rf/dispatch-sync [::tree/node-select-default
                     ::identifier
                     form-db-path [form-db-path
                                   preview-db-path]])
  (layout/three-columns
   [:<>
    [preview :original? true]
    [edit-controls]]
   [:<>
    [form/active {::tree/identifier ::identifier}]
    [message/display entity-type]
    [buttons/buttons entity-type
     [svg-buttons form-db-path]]
    [attribution/attribution {:path form-db-path}]]
   [:<>
    [history/buttons form-db-path]
    [tree/tree
     ::identifier
     [form-db-path
      :spacer
      (conj preview-db-path :render-options)
      :spacer
      (conj preview-db-path :coat-of-arms :field :components 0)]]
    [preview]]
   :banner (let [entity-id @(rf/subscribe [:get (conj form-db-path :id)])
                 entity-version @(rf/subscribe [:get (conj form-db-path :version)])]
             [details/latest-version-banner
              entity-id
              entity-version
              (library.charge.list/on-select {:id entity-id})])))

(defn create-view []
  [details/create-view entity-type charge-form #(go default/charge-entity)])

(def ^:private entity-id-cache-path
  [:ui :charge-edit :entity-id])

(rf/reg-event-db
  ::clear-charge-type-data
  (fn [db [_ id version]]
    (-> db
        (assoc-in entity-id-cache-path [id version])
        (repository.charge-types/clear)
        (tree/clear ::charge-type-select/identifier))))

(defn details-view [{{{:keys [id version]} :path} :parameters}]
  ;; this makes sure to clear the charge-type select field's state and the charge-types data in
  ;; the repository, because the loaded charge might have a new charge-type that doesn't exist in
  ;; the cached data; it's not an ideal mechanism
  (when (not= [id version] @(rf/subscribe [:get entity-id-cache-path]))
    (rf/dispatch-sync [::clear-charge-type-data id version]))
  [details/by-id-view (str "charge:" id) version charge-form])
