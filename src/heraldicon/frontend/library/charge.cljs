(ns heraldicon.frontend.library.charge
  (:require
   ["copy-to-clipboard" :as copy-to-clipboard]
   ["svgo-browser/lib/get-svgo-instance" :as getSvgoInstance]
   [cljs.core.async :refer [go]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [clojure.set :as set]
   [clojure.string :as s]
   [clojure.walk :as walk]
   [com.wsscode.async.async-cljs :refer [<? go-catch]]
   [heraldicon.context :as c]
   [heraldicon.entity.attribution :as entity.attribution]
   [heraldicon.entity.id :as id]
   [heraldicon.frontend.api :as api]
   [heraldicon.frontend.api.request :as api.request]
   [heraldicon.frontend.attribution :as attribution]
   [heraldicon.frontend.form :as form]
   [heraldicon.frontend.history.core :as history]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.layout :as layout]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.modal :as modal]
   [heraldicon.frontend.not-found :as not-found]
   [heraldicon.frontend.state :as state]
   [heraldicon.frontend.svgo-setup] ;; needed for side effects
   [heraldicon.frontend.ui.core :as ui]
   [heraldicon.frontend.ui.element.charge-select :as charge-select]
   [heraldicon.frontend.ui.element.hover-menu :as hover-menu]
   [heraldicon.frontend.ui.shared :as shared]
   [heraldicon.frontend.user :as user]
   [heraldicon.heraldry.default :as default]
   [heraldicon.localization.string :as string]
   [heraldicon.render.core :as render]
   [heraldicon.svg.core :as svg]
   [heraldicon.util.colour :as colour]
   [hickory.core :as hickory]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]
   [taoensso.timbre :as log]))

(def form-id
  :heraldicon.entity/charge)

(def form-db-path
  (form/data-path form-id))

(history/register-undoable-path form-db-path)

(def ^:private saved-data-db-path
  [:saved-charge-data])

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

(defn- load-svg-file [db-path data]
  (go-catch
   (try
     (-> data
         (svg/optimize (fn [options data]
                         (go-catch
                          (-> options
                              getSvgoInstance
                              (.optimize data)
                              <p!))))
         <?
         hickory/parse-fragment
         first
         hickory/as-hiccup
         svg/process-style-blocks
         svg/strip-unnecessary-parts
         svg/fix-string-style-values
         svg/fix-attribute-and-tag-names
         (as-> parsed
               (let [edn-data (-> parsed
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
                                  svg/add-ids)
                     width (-> parsed
                               (get-in [1 :width])
                               parse-number-with-unit)
                     height (-> parsed
                                (get-in [1 :height])
                                parse-number-with-unit)
                     [shift-x shift-y
                      width height] (let [[viewbox-x viewbox-y
                                           viewbox-width viewbox-height] (-> parsed
                                                                             (get-in [1 :viewbox])
                                                                             parse-width-height-from-viewbox)]
                                      (if (and viewbox-width viewbox-height)
                                        [viewbox-x viewbox-y viewbox-width viewbox-height]
                                        [0 0 width height]))
                     [shift-x shift-y] (if (and shift-x shift-y)
                                         [shift-x shift-y]
                                         [100 100])
                     [width height] (if (and width height)
                                      [width height]
                                      [100 100])
                     edn-data (assoc-in edn-data
                                        [1 :transform] (str "translate(" (- shift-x) "," (- shift-y) ")"))
                     existing-colours @(rf/subscribe [:get (conj db-path :colours)])]
                 (rf/dispatch [:merge db-path (update-colours-map
                                               {:colours existing-colours
                                                :edn-data {:data edn-data
                                                           :width width
                                                           :height height}
                                                :svg-data data})]))))
     (rf/dispatch [::clear-selected-colours])
     (catch :default e
       (log/error "load svg file error:" e)))))

(def ^:private show-colours-path
  [:ui :colours :show])

(macros/reg-event-db ::toggle-select-colour
  (fn [db [_ colour]]
    (update-in db (conj show-colours-path colour) not)))

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
  (fn [db [_ element-id]]
    (let [colours (set (keys (get-in db (conj form-db-path :data :colours))))
          new-colour (generate-new-colour colours)]
      (-> db
          (update-in (conj form-db-path :data :edn-data :data) (partial colourize-element element-id new-colour))
          (update-in (conj form-db-path :data) update-colours-map)))))

(defn- svg-fill-clicked [element event]
  (doto event
    .preventDefault
    .stopPropagation)
  (if @(rf/subscribe [::colourize-mode?])
    (rf/dispatch [::colourize-element (-> element second :id)])
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

(defn- prepare-for-preview [data]
  (update-in data
             [:data :edn-data :data]
             (fn [charge-data]
               (walk/postwalk (fn [element]
                                (if (and (vector? element)
                                         (-> element second map?)
                                         (-> element second :fill))
                                  (-> element
                                      (assoc-in [1 :on-click] (partial svg-fill-clicked element))
                                      (assoc-in [1 :style :cursor] "pointer"))
                                  element))
                              charge-data))))

(defn- preview [& {:keys [original?]}]
  (let [form-data @(rf/subscribe [:get form-db-path])
        prepared-charge-data (-> form-data
                                 (update :username #(or % (:username (user/data))))
                                 (cond->
                                   original? prepare-for-preview))
        coat-of-arms @(rf/subscribe [:get (conj example-coa-db-path :coat-of-arms)])
        {:keys [result
                environment]} (render/coat-of-arms
                               (-> shared/coa-select-option-context
                                   (c/<< :path [:context :coat-of-arms])
                                   (c/<< :ui-show-colours
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
                                   (c/<< :charge-preview? true)
                                   (c/<< :preview-original? original?))
                               100)
        {:keys [width height]} environment]
    [:svg {:viewBox (str "0 0 " (-> width (* 5) (+ 20)) " " (-> height (* 5) (+ 20) (+ 20)))
           :preserveAspectRatio "xMidYMid meet"
           :style {:width "100%"}}
     [:g {:transform "translate(10,10)"}
      [:g {:transform "scale(5,5)"}
       result]]]))

(defn- upload-file [event]
  (modal/start-loading)
  (let [file (-> event .-target .-files (.item 0))]
    (when file
      (let [reader (js/FileReader.)]
        (set! (.-onloadend reader) (fn []
                                     (let [raw-data (.-result reader)]
                                       (load-svg-file (conj form-db-path :data) raw-data))
                                     (modal/stop-loading)))
        (set! (-> event .-target .-value) "")
        (.readAsText reader file)))))

(defn- save-charge-clicked [event]
  (.preventDefault event)
  (.stopPropagation event)
  (let [payload (update-in @(rf/subscribe [:get form-db-path])
                           [:data :edn-data :data]
                           svg/strip-unnecessary-parts)
        user-data (user/data)]
    (rf/dispatch-sync [::form/clear-messages form-id])
    (go
      (try
        (modal/start-loading)
        (let [response (<? (api.request/call :save-charge payload user-data))
              charge-id (:id response)]
          (rf/dispatch-sync [:set (conj form-db-path :id) charge-id])
          (rf/dispatch-sync [:set saved-data-db-path @(rf/subscribe [:get form-db-path])])
          (state/invalidate-cache-without-current form-db-path [charge-id nil])
          (state/invalidate-cache-without-current form-db-path [charge-id 0])
          (charge-select/invalidate-charges-cache)
          (rf/dispatch-sync [::form/set-message
                             form-id
                             (string/str-tr :string.user.message/charge-saved " " (:version response))])
          (reife/push-state :route.charge.details/by-id {:id (id/for-url charge-id)}))
        (modal/stop-loading)
        (catch :default e
          (log/error "save-form error:" e)
          (rf/dispatch-sync [::form/set-error form-id (:message (ex-data e))])
          (modal/stop-loading))))))

(defn- copy-to-new-clicked []
  (let [charge-data @(rf/subscribe [:get form-db-path])]
    (rf/dispatch-sync [::form/clear-messages form-id])
    (state/set-async-fetch-data
     form-db-path
     :new
     (dissoc charge-data
             :id
             :version
             :latest-version
             :username
             :user-id
             :created-at
             :first-version-created-at
             :name))
    (rf/dispatch-sync [::form/set-message form-id :string.user.message/created-unsaved-copy])
    (reife/push-state :route.charge/create)))

(defn- share-button-clicked []
  (let [url (entity.attribution/full-url-for-charge {:path form-db-path})]
    (rf/dispatch-sync [::form/clear-messages form-id])
    (if (copy-to-clipboard url)
      (rf/dispatch-sync [::form/set-message form-id :string.user.message/copied-url-for-sharing])
      (rf/dispatch-sync [::form/set-error form-id :string.user.message/copy-to-clipboard-failed]))))

(defn- button-row []
  (let [charge-id @(rf/subscribe [:get (conj form-db-path :id)])
        charge-username @(rf/subscribe [:get (conj form-db-path :username)])
        charge-svg-url @(rf/subscribe [:get (conj form-db-path :data :svg-data-url)])
        public? (= @(rf/subscribe [:get (conj form-db-path :access)])
                   :public)
        user-data (user/data)
        logged-in? (:logged-in? user-data)
        unsaved-changes? (not= @(rf/subscribe [:get form-db-path])
                               @(rf/subscribe [:get saved-data-db-path]))
        saved? charge-id
        owned-by-me? (= (:username user-data) charge-username)
        can-copy? (and logged-in?
                       saved?
                       owned-by-me?)
        can-save? (and logged-in?
                       (or (not saved?)
                           owned-by-me?))
        can-upload? can-save?
        can-share? (and public?
                        saved?
                        (not unsaved-changes?))]
    [:<>
     [form/messages]

     [:div.buttons {:style {:display "flex"}}
      [:label.button {:for "upload"
                      :disabled (not can-upload?)
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
                             upload-file)
                :style {:display "none"}}]]
      (when charge-svg-url
        [:a {:href charge-svg-url
             :target "_blank"
             :style {:flex "initial"
                     :padding-top "0.5em"
                     :white-space "nowrap"}}
         [tr :string.miscellaneous/svg-file]])
      [:div {:style {:flex "auto"}}]
      [:button.button {:style {:flex "initial"
                               :color "#777"}
                       :class (when-not can-share? "disabled")
                       :title (when-not can-share? (tr :string.user.message/arms-need-to-be-public-and-saved-for-sharing))
                       :on-click (when can-share?
                                   share-button-clicked)}
       [:i.fas.fa-share-alt]]
      [hover-menu/hover-menu
       {:path [:arms-form-action-menu]}
       :string.button/actions
       [{:title :string.button/copy-to-new
         :icon "fas fa-clone"
         :handler copy-to-new-clicked
         :disabled? (not can-copy?)
         :tooltip (when-not can-copy?
                    (tr :string.user.message/need-to-be-logged-in-and-arms-must-be-saved))}
        {:title :string.button/share
         :icon "fas fa-share-alt"
         :handler share-button-clicked
         :disabled? (not can-share?)
         :tooltip (when-not can-share?
                    (tr :string.user.message/arms-need-to-be-public-and-saved-for-sharing))}]
       [:button.button {:style {:flex "initial"
                                :color "#777"
                                :margin-left "10px"}}
        [:i.fas.fa-ellipsis-h]]
       :require-click? true]
      [:button.button.primary
       {:type "submit"
        :class (when-not can-save? "disabled")
        :on-click (if can-save?
                    save-charge-clicked
                    #(js/alert (tr :string.user.message/need-to-be-logged-in-and-own-the-charge.)))
        :style {:flex "initial"
                :margin-left "10px"}}
       [tr :string.button/save]]]]))

(defn- attribution []
  (let [attribution-data (attribution/for-charge {:path form-db-path})]
    [:div.attribution
     [:h3 [tr :string.attribution/title]]
     [:div {:style {:padding-left "1em"}}
      attribution-data]]))

(defn- charge-form []
  (rf/dispatch [:set-title-from-path-or-default
                (conj form-db-path :name)
                :string.text.title/create-charge])
  (rf/dispatch-sync [:ui-component-node-select-default form-db-path [form-db-path
                                                                     example-coa-db-path]])
  (layout/three-columns
   [:<>
    [preview :original? true]
    [edit-controls]]
   [:<>
    [ui/selected-component]
    [button-row]
    [attribution]]
   [:<>
    [history/buttons form-db-path]
    [ui/component-tree [form-db-path
                        :spacer
                        (conj example-coa-db-path :render-options)
                        :spacer
                        (conj example-coa-db-path :coat-of-arms :field :components 0)]]
    [preview]]))

(defn- load-charge [charge-id version]
  (when @(rf/subscribe [:heraldicon.frontend.history.core/identifier-changed? form-db-path charge-id])
    (rf/dispatch-sync [:heraldicon.frontend.history.core/clear form-db-path charge-id]))
  (let [[status charge-data] (state/async-fetch-data
                              form-db-path
                              [charge-id version]
                              #(api/fetch-charge-for-editing charge-id version saved-data-db-path))]
    (when (= status :done)
      (if charge-data
        [charge-form]
        [not-found/not-found]))))

(defn create-view [_match]
  (when @(rf/subscribe [:heraldicon.frontend.history.core/identifier-changed? form-db-path nil])
    (rf/dispatch-sync [:heraldicon.frontend.history.core/clear form-db-path nil]))
  (let [[status _charge-form-data] (state/async-fetch-data
                                    form-db-path
                                    :new
                                    #(go default/charge-entity))]
    (when (= status :done)
      [charge-form])))

(defn on-select [{:keys [id]}]
  {:href (reife/href :route.charge.details/by-id {:id (id/for-url id)})
   :on-click (fn [_event]
               (rf/dispatch-sync [::form/clear-messages form-id]))})

(defn list-view []
  (rf/dispatch [:set-title :string.entity/charges])
  [:div {:style {:padding "15px"}}
   [:div {:style {:text-align "justify"
                  :max-width "40em"}}
    [:p [tr :string.text.charge-library/create-and-view-charges]]]
   [:button.button.primary
    {:on-click #(do
                  (rf/dispatch-sync [::form/clear-messages form-id])
                  (reife/push-state :route.charge/create))}
    [tr :string.button/create]]
   [:div {:style {:padding-top "0.5em"}}
    [charge-select/list-charges on-select]]])

(defn details-view [{{{:keys [id version]} :path} :parameters}]
  [load-charge (str "charge:" id) version])
