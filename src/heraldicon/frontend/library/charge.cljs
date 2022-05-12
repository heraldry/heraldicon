(ns heraldicon.frontend.library.charge
  (:require
   ["svgo-browser/lib/get-svgo-instance" :as getSvgoInstance]
   [cljs.core.async :refer [go]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [clojure.set :as set]
   [clojure.string :as s]
   [clojure.walk :as walk]
   [com.wsscode.async.async-cljs :refer [<? go-catch]]
   [heraldicon.colour :as colour]
   [heraldicon.context :as c]
   [heraldicon.entity.id :as id]
   [heraldicon.frontend.api.request :as api.request]
   [heraldicon.frontend.attribution :as attribution]
   [heraldicon.frontend.charge :as charge]
   [heraldicon.frontend.history.core :as history]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.modal :as modal]
   [heraldicon.frontend.not-found :as not-found]
   [heraldicon.frontend.state :as state]
   [heraldicon.frontend.ui.core :as ui]
   [heraldicon.frontend.ui.element.charge-select :as charge-select]
   [heraldicon.frontend.ui.shared :as shared]
   [heraldicon.frontend.user :as user]
   [heraldicon.localization.string :as string]
   [heraldicon.render.core :as render]
   [heraldicon.svg.core :as svg]
   [heraldicon.util.core :as util]
   [hickory.core :as hickory]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]
   [taoensso.timbre :as log]))

(def form-db-path
  [:charge-form])

(def list-db-path
  [:charge-list])

(def example-coa-db-path
  [:example-coa])

(defn find-colours [data]
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

;; views

(defn parse-number-with-unit [s]
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

(defn parse-width-height-from-viewbox [s]
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

(defn load-svg-file [db-path data]
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
         svg/strip-style-block
         svg/strip-classes
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
                                            data)))))
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
                     colours (into {}
                                   (map (fn [c]
                                          [c :keep]) (find-colours
                                                      edn-data)))
                     edn-data (-> edn-data
                                  (assoc-in [1 :transform] (str "translate(" (- shift-x) "," (- shift-y) ")")))]
                 (let [existing-colours @(rf/subscribe [:get (conj db-path :colours)])
                       new-colours (merge colours
                                          (select-keys existing-colours
                                                       (set/intersection
                                                        (-> colours
                                                            keys
                                                            set)
                                                        (-> existing-colours
                                                            keys
                                                            set))))]
                   (rf/dispatch [:set (conj db-path :colours) new-colours]))
                 (rf/dispatch [:set (conj db-path :data) {:edn-data {:data edn-data
                                                                     :width width
                                                                     :height height}
                                                          :svg-data data}]))))
     (catch :default e
       (log/error "load svg file error:" e)))))

(defn preview []
  (let [{:keys [data]
         :as form-data} @(rf/subscribe [:get form-db-path])
        {:keys [edn-data]} data
        prepared-charge-data (-> form-data
                                 (assoc :data edn-data)
                                 (update :username #(or % (:username (user/data)))))
        coat-of-arms @(rf/subscribe [:get (conj example-coa-db-path :coat-of-arms)])
        {:keys [result
                environment]} (render/coat-of-arms
                               (-> shared/coa-select-option-context
                                   (c/<< :path [:context :coat-of-arms])
                                   (c/<< :ui-show-colours
                                         (->> @(rf/subscribe [:get [:ui :colours :show]])
                                              (keep (fn [value]
                                                      (when (second value)
                                                        (first value))))
                                              set))
                                   (c/<< :render-options-path
                                         (conj example-coa-db-path :render-options))
                                   (c/<< :coat-of-arms
                                         (-> coat-of-arms
                                             (assoc-in [:field :components 0 :data] prepared-charge-data))))
                               100)
        {:keys [width height]} environment]
    [:svg {:viewBox (str "0 0 " (-> width (* 5) (+ 20)) " " (-> height (* 5) (+ 20) (+ 20)))
           :preserveAspectRatio "xMidYMid meet"
           :style {:width "100%"}}
     [:g {:transform "translate(10,10)"}
      [:g {:transform "scale(5,5)"}
       result]]]))

(defn upload-file [event]
  (modal/start-loading)
  (let [file (-> event .-target .-files (.item 0))]
    (when file
      (let [reader (js/FileReader.)]
        (set! (.-onloadend reader) (fn []
                                     (let [raw-data (.-result reader)]
                                       (load-svg-file form-db-path raw-data))
                                     (modal/stop-loading)))
        (set! (-> event .-target .-value) "")
        (.readAsText reader file)))))

(defn invalidate-charges-cache []
  (let [user-data (user/data)
        user-id (:user-id user-data)]
    (rf/dispatch-sync [:set list-db-path nil])
    (state/invalidate-cache list-db-path user-id)
    (state/invalidate-cache [:all-charges] :all-charges)))

(defn save-charge-clicked [event]
  (.preventDefault event)
  (.stopPropagation event)
  (let [payload @(rf/subscribe [:get form-db-path])
        user-data (user/data)]
    (rf/dispatch-sync [:clear-form-errors form-db-path])
    (rf/dispatch-sync [:clear-form-message form-db-path])
    (go
      (try
        (modal/start-loading)
        (let [response (<? (api.request/call :save-charge payload user-data))
              charge-id (-> response :charge-id)]
          (rf/dispatch-sync [:set (conj form-db-path :id) charge-id])
          (state/invalidate-cache-without-current form-db-path [charge-id nil])
          (state/invalidate-cache-without-current form-db-path [charge-id 0])
          (invalidate-charges-cache)
          (rf/dispatch-sync [:set-form-message form-db-path
                             (string/str-tr :string.user.message/charge-saved (:version response))])
          (reife/push-state :view-charge-by-id {:id (id/for-url charge-id)}))
        (modal/stop-loading)
        (catch :default e
          (log/error "save-form error:" e)
          (rf/dispatch [:set-form-error form-db-path (:message (ex-data e))])
          (modal/stop-loading))))))

(defn copy-to-new-clicked [event]
  (.preventDefault event)
  (.stopPropagation event)
  (let [charge-data @(rf/subscribe [:get form-db-path])]
    (rf/dispatch-sync [:clear-form-errors form-db-path])
    (state/set-async-fetch-data
     form-db-path
     :new
     (-> charge-data
         (dissoc :id)
         (dissoc :version)
         (dissoc :latest-version)
         (dissoc :username)
         (dissoc :user-id)
         (dissoc :created-at)
         (dissoc :first-version-created-at)
         (dissoc :is-current-version)
         (dissoc :name)))
    (rf/dispatch-sync [:set-form-message form-db-path :string.user.message/created-unsaved-copy])
    (reife/push-state :create-charge)))

(defn button-row []
  (let [error-message @(rf/subscribe [:get-form-error form-db-path])
        form-message @(rf/subscribe [:get-form-message form-db-path])
        charge-id @(rf/subscribe [:get (conj form-db-path :id)])
        charge-username @(rf/subscribe [:get (conj form-db-path :username)])
        charge-svg-url @(rf/subscribe [:get (conj form-db-path :svg-data-url)])
        user-data (user/data)
        logged-in? (:logged-in? user-data)
        saved? charge-id
        owned-by-me? (= (:username user-data) charge-username)
        can-copy? (and logged-in?
                       saved?
                       owned-by-me?)
        can-save? (and logged-in?
                       (or (not saved?)
                           owned-by-me?))
        can-upload? can-save?]
    [:<>
     (when form-message
       [:div.success-message [tr form-message]])
     (when error-message
       [:div.error-message error-message])

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
      [:button.button
       {:type "button"
        :class (when-not can-copy? "disabled")
        :style {:flex "initial"
                :margin-left "10px"}
        :on-click (if can-copy?
                    copy-to-new-clicked
                    #(js/alert (tr :string.user.message/need-to-be-logged-in-and-charge-must-be-saved)))}
       [tr :string.button/copy-to-new]]
      [:button.button.primary
       {:type "submit"
        :class (when-not can-save? "disabled")
        :on-click (if can-save?
                    save-charge-clicked
                    #(js/alert (tr :string.user.message/need-to-be-logged-in-and-own-the-charge.)))
        :style {:flex "initial"
                :margin-left "10px"}}
       [tr :string.button/save]]]]))

(defn attribution []
  (let [attribution-data (attribution/for-charge {:path form-db-path})]
    [:div.attribution
     [:h3 [tr :string.attribution/title]]
     [:div {:style {:padding-left "1em"}}
      attribution-data]]))

(defn charge-form []
  (rf/dispatch [:set-title-from-path-or-default
                (conj form-db-path :name)
                :string.text.title/create-charge])
  (rf/dispatch-sync [:ui-component-node-select-default form-db-path [form-db-path
                                                                     example-coa-db-path]])
  [:div {:style {:display "grid"
                 :grid-gap "10px"
                 :grid-template-columns "[start] auto [first] minmax(26em, 33%) [second] minmax(10em, 25%) [end]"
                 :grid-template-rows "[top] 100% [bottom]"
                 :grid-template-areas "'left middle right'"
                 :padding-right "10px"
                 :height "100%"}
         :on-click #(state/dispatch-on-event % [:ui-submenu-close-all])}
   [:div.no-scrollbar {:style {:grid-area "left"}}
    [preview]]
   [:div.no-scrollbar {:style {:grid-area "middle"
                               :padding-top "10px"}}
    [ui/selected-component]
    [button-row]
    [attribution]]
   [:div.no-scrollbar {:style {:grid-area "right"
                               :padding-top "5px"
                               :position "relative"}}
    [history/buttons form-db-path]
    [ui/component-tree [form-db-path
                        (conj example-coa-db-path :render-options)
                        (conj example-coa-db-path :coat-of-arms :field :components 0)]]]])

(defn charge-display [charge-id version]
  (when @(rf/subscribe [:heraldicon.frontend.history.core/identifier-changed? form-db-path charge-id])
    (rf/dispatch-sync [:heraldicon.frontend.history.core/clear form-db-path charge-id]))
  (let [[status charge-data] (state/async-fetch-data
                              form-db-path
                              [charge-id version]
                              #(charge/fetch-charge-for-editing charge-id version))]
    (when (= status :done)
      (if charge-data
        [charge-form]
        [not-found/not-found]))))

(defn create-charge [_match]
  (when @(rf/subscribe [:heraldicon.frontend.history.core/identifier-changed? form-db-path nil])
    (rf/dispatch-sync [:heraldicon.frontend.history.core/clear form-db-path nil]))
  (let [[status _charge-form-data] (state/async-fetch-data
                                    form-db-path
                                    :new
                                    #(go
                                       ;; TODO: make a default charge here?
                                       {}))]
    (when (= status :done)
      [charge-form])))

(defn on-select [{:keys [id]}]
  {:href (reife/href :view-charge-by-id {:id (id/for-url id)})
   :on-click (fn [_event]
               (rf/dispatch-sync [:clear-form-errors form-db-path])
               (rf/dispatch-sync [:clear-form-message form-db-path]))})

(defn list-all-charges []
  [charge-select/list-charges on-select])

(defn view-list-charges []
  (rf/dispatch [:set-title :string.entity/charges])
  [:div {:style {:padding "15px"}}
   [:div {:style {:text-align "justify"
                  :max-width "40em"}}
    [:p
     [tr :string.text.charge-library/create-and-view-charges]]]
   [:button.button.primary
    {:on-click #(do
                  (rf/dispatch-sync [:clear-form-errors form-db-path])
                  (rf/dispatch-sync [:clear-form-message form-db-path])
                  (reife/push-state :create-charge))}
    [tr :string.button/create]]
   [:div {:style {:padding-top "0.5em"}}
    [list-all-charges]]])

(defn view-charge-by-id [{:keys [parameters]}]
  (let [id (-> parameters :path :id)
        version (-> parameters :path :version)
        charge-id (str "charge:" id)]
    (if (or (nil? version)
            (util/integer-string? version))
      [charge-display charge-id version]
      [not-found/not-found])))
