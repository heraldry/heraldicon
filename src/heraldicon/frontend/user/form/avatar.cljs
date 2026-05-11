(ns heraldicon.frontend.user.form.avatar
  (:require
   [cljs.core.async :as async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.frontend.api :as api]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.message :as message]
   [heraldicon.frontend.modal :as modal]
   [heraldicon.frontend.repository.user :as repository.user]
   [heraldicon.frontend.user.form.core :as form]
   [heraldicon.frontend.user.session :as session]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [taoensso.timbre :as log]))

(def ^:private form-id ::id)

(def ^:private crop-size 256)

(def ^:private max-output-size 256)

(defn- cover-fit-scale [natural-w natural-h]
  (max (/ crop-size natural-w)
       (/ crop-size natural-h)))

(defn- effective-scale [{:keys [scale source]}]
  (let [{:keys [natural-w natural-h]} source]
    (* (or scale 1) (cover-fit-scale natural-w natural-h))))

(defn- clamp [{:keys [tx ty source] :as state}]
  (let [{:keys [natural-w natural-h]} source
        s (effective-scale state)
        min-tx (- crop-size (* natural-w s))
        min-ty (- crop-size (* natural-h s))]
    (assoc state
           :tx (-> (or tx 0) (max min-tx) (min 0))
           :ty (-> (or ty 0) (max min-ty) (min 0)))))

(rf/reg-event-db ::set-source
  (fn [db [_ source]]
    (-> db
        (assoc-in (form/form-path form-id)
                  (clamp {:source source
                          :scale 1
                          :tx (/ (- crop-size (* (:natural-w source)
                                                 (cover-fit-scale (:natural-w source) (:natural-h source)))) 2)
                          :ty (/ (- crop-size (* (:natural-h source)
                                                 (cover-fit-scale (:natural-w source) (:natural-h source)))) 2)
                          :uncropped? false})))))

(rf/reg-event-db ::clear-source
  (fn [db _]
    (update-in db (form/form-path form-id) dissoc :source :scale :tx :ty :drag)))

(rf/reg-event-db ::start-drag
  (fn [db [_ pointer-x pointer-y]]
    (let [state (get-in db (form/form-path form-id))]
      (assoc-in db (form/form-path form-id)
                (assoc state :drag {:pointer-x pointer-x
                                    :pointer-y pointer-y
                                    :start-tx (:tx state)
                                    :start-ty (:ty state)})))))

(rf/reg-event-db ::drag-to
  (fn [db [_ pointer-x pointer-y]]
    (let [state (get-in db (form/form-path form-id))
          {:keys [drag]} state]
      (if drag
        (assoc-in db (form/form-path form-id)
                  (clamp (assoc state
                                :tx (+ (:start-tx drag) (- pointer-x (:pointer-x drag)))
                                :ty (+ (:start-ty drag) (- pointer-y (:pointer-y drag))))))
        db))))

(rf/reg-event-db ::end-drag
  (fn [db _]
    (update-in db (form/form-path form-id) dissoc :drag)))

(defn- zoom-to-center [state new-scale]
  (let [old-scale (or (:scale state) 1)
        center (/ crop-size 2)
        ratio (/ new-scale old-scale)]
    (assoc state
           :scale new-scale
           :tx (+ center (* ratio (- (or (:tx state) 0) center)))
           :ty (+ center (* ratio (- (or (:ty state) 0) center))))))

(rf/reg-event-db ::set-scale
  (fn [db [_ new-scale]]
    (let [state (get-in db (form/form-path form-id))]
      (assoc-in db (form/form-path form-id)
                (clamp (zoom-to-center state (max 1 (min 5 new-scale))))))))

(rf/reg-event-db ::adjust-scale
  (fn [db [_ delta]]
    (let [state (get-in db (form/form-path form-id))
          new-scale (max 1 (min 5 (+ (or (:scale state) 1) delta)))]
      (assoc-in db (form/form-path form-id)
                (clamp (zoom-to-center state new-scale))))))

(rf/reg-event-db ::set-uncropped
  (fn [db [_ uncropped?]]
    (assoc-in db (conj (form/form-path form-id) :uncropped?) (boolean uncropped?))))

(defn- read-file [^js file]
  (let [reader (js/FileReader.)]
    (set! (.-onloadend reader)
          (fn []
            (let [data-url (.-result reader)
                  img (js/Image.)]
              (set! (.-onload img)
                    (fn []
                      (rf/dispatch [::set-source
                                    {:data-url data-url
                                     :natural-w (.-naturalWidth img)
                                     :natural-h (.-naturalHeight img)
                                     :mime (.-type file)}])))
              (set! (.-src img) data-url))))
    (.readAsDataURL reader file)))

(defn- on-file-input-change [event]
  (let [file (some-> event .-target .-files (.item 0))]
    (when file
      (read-file file))
    (set! (-> event .-target .-value) "")))

(defn- output-content-type [source-mime]
  (if (= source-mime "image/jpeg")
    "image/jpeg"
    "image/png"))

(defn- render-cropped-canvas [{:keys [source] :as state}]
  (let [s (effective-scale state)
        canvas (js/document.createElement "canvas")
        ctx (.getContext canvas "2d")
        img (js/Image.)
        out (async/chan)]
    (set! (.-width canvas) max-output-size)
    (set! (.-height canvas) max-output-size)
    (set! (.-onload img)
          (fn []
            (.drawImage ctx img
                        (/ (- (:tx state)) s)
                        (/ (- (:ty state)) s)
                        (/ crop-size s)
                        (/ crop-size s)
                        0 0
                        max-output-size
                        max-output-size)
            (.toBlob canvas
                     (fn [blob]
                       (async/put! out blob))
                     (output-content-type (:mime source)))))
    (set! (.-src img) (:data-url source))
    out))

(defn- blob->base64 [^js blob]
  (let [out (async/chan)
        reader (js/FileReader.)]
    (set! (.-onloadend reader)
          (fn []
            (let [data-url (.-result reader)
                  comma (.indexOf data-url ",")]
              (async/put! out (.substring data-url (inc comma))))))
    (.readAsDataURL reader blob)
    out))

(rf/reg-event-fx ::submit
  (fn [{:keys [db]} _]
    (let [state (get-in db (form/form-path form-id))]
      (cond-> {:dispatch-n [[::message/clear form-id]]}
        (not (:source state)) (update :dispatch-n conj
                                      [::message/set-error form-id
                                       :string.user.message/avatar-required])
        (:source state) (assoc ::upload state)))))

(rf/reg-fx ::upload
  (fn [{:keys [source uncropped?] :as state}]
    (modal/start-loading)
    (go
      (try
        (let [blob (<? (render-cropped-canvas state))
              base64 (<? (blob->base64 blob))
              user (<? (api/call :update-avatar
                                 {:image-data base64
                                  :uncropped? (boolean uncropped?)}
                                 @(rf/subscribe [::session/data])))]
          (rf/dispatch [::repository.user/store user])
          (rf/dispatch [::form/clear-and-close form-id]))
        (catch :default e
          (log/error e "avatar upload error" {:mime (:mime source)})
          (rf/dispatch [::message/set-error form-id
                        (or (:message (ex-data e))
                            :string.user.message/avatar-upload-failed)]))
        (finally
          (modal/stop-loading))))))

(rf/reg-fx ::delete
  (fn [_]
    (modal/start-loading)
    (go
      (try
        (let [user (<? (api/call :delete-avatar nil @(rf/subscribe [::session/data])))]
          (rf/dispatch [::repository.user/store user])
          (rf/dispatch [::form/clear-and-close form-id]))
        (catch :default e
          (log/error e "avatar delete error")
          (rf/dispatch [::message/set-error form-id
                        (or (:message (ex-data e))
                            :string.user.message/avatar-delete-failed)]))
        (finally
          (modal/stop-loading))))))

(rf/reg-event-fx ::remove
  (fn [_ _]
    {::delete nil}))

(defn- file-input []
  [:div {:style {:margin-bottom "10px"}}
   [:label.button {:for "avatar-file-upload"
                   :style {:display "inline-block"
                           :width "auto"}}
    [tr :string.user.avatar/choose-file]
    [:input {:type "file"
             :id "avatar-file-upload"
             :accept "image/png,image/jpeg"
             :on-change on-file-input-change
             :style {:display "none"}}]]])

(defn- crop-preview []
  (let [el-ref (atom nil)
        wheel-handler (fn [^js e]
                        (.preventDefault e)
                        (.stopPropagation e)
                        (rf/dispatch-sync [::adjust-scale (- (* (.-deltaY e) 0.002))]))]
    (r/create-class
     {:display-name "avatar-crop-preview"
      :component-did-mount (fn [_]
                             (when-let [el @el-ref]
                               (.addEventListener el "wheel" wheel-handler #js {:passive false})))
      :component-will-unmount (fn [_]
                                (when-let [el @el-ref]
                                  (.removeEventListener el "wheel" wheel-handler)))
      :reagent-render
      (fn [state]
        (let [{:keys [source uncropped?]} state
              s (effective-scale state)]
          [:div {:ref (fn [el] (reset! el-ref el))
                 :style {:position "relative"
                         :width (str crop-size "px")
                         :height (str crop-size "px")
                         :overflow "hidden"
                         :background "#222"
                         :user-select "none"
                         :touch-action "none"
                         :cursor "grab"}
                 :on-pointer-down (fn [^js e]
                                    (.preventDefault e)
                                    (-> e .-currentTarget (.setPointerCapture (.-pointerId e)))
                                    (rf/dispatch-sync [::start-drag (.-clientX e) (.-clientY e)]))
                 :on-pointer-move (fn [^js e]
                                    (rf/dispatch-sync [::drag-to (.-clientX e) (.-clientY e)]))
                 :on-pointer-up (fn [^js e]
                                  (-> e .-currentTarget (.releasePointerCapture (.-pointerId e)))
                                  (rf/dispatch-sync [::end-drag]))
                 :on-pointer-cancel (fn [_]
                                      (rf/dispatch-sync [::end-drag]))}
           [:img {:src (:data-url source)
                  :style {:position "absolute"
                          :left 0
                          :top 0
                          :width (str (:natural-w source) "px")
                          :height (str (:natural-h source) "px")
                          :transform (str "translate(" (:tx state) "px, " (:ty state) "px) scale(" s ")")
                          :transform-origin "0 0"
                          :pointer-events "none"}
                  :draggable false}]
           (when-not uncropped?
             (let [stripes "repeating-linear-gradient(135deg, rgba(0,0,0,0.55) 0 8px, rgba(255,255,255,0.35) 8px 16px)"
                   mask "radial-gradient(circle closest-side at center, transparent calc(100% - 1px), black 100%)"]
               [:div {:style {:position "absolute"
                              :left 0
                              :top 0
                              :width (str crop-size "px")
                              :height (str crop-size "px")
                              :pointer-events "none"
                              :background-image stripes
                              :-webkit-mask-image mask
                              :mask-image mask}}]))]))})))

(defn- controls [{:keys [scale uncropped?]}]
  [:div {:style {:display "flex"
                 :flex-direction "column"
                 :gap "8px"
                 :width (str crop-size "px")}}
   [:div {:style {:color "#888"
                  :font-size "0.85em"}}
    [tr :string.user.avatar/instructions]]
   [:input {:type "range"
            :aria-label "Zoom"
            :min 1
            :max 5
            :step 0.01
            :value scale
            :on-change #(rf/dispatch-sync [::set-scale (-> % .-target .-value js/parseFloat)])}]
   [:label
    [:input {:type "checkbox"
             :checked (boolean uncropped?)
             :on-change #(rf/dispatch [::set-uncropped (-> % .-target .-checked)])}]
    " "
    [tr :string.user.avatar/no-circle-crop]]])

(defn- form []
  (let [state @(rf/subscribe [:get (form/form-path form-id)])]
    [:form {:on-submit (form/on-submit-fn [::submit])
            :style {:display "flex"
                    :flex-direction "column"
                    :align-items "center"
                    :gap "10px"}}
     [message/display form-id]
     [file-input]
     (when (:source state)
       [:<>
        [crop-preview state]
        [controls state]])
     [:div {:style {:text-align "right"
                    :margin-top "10px"}}
      [:button.button {:style {:margin-right "5px"}
                       :type "reset"
                       :on-click #(rf/dispatch [::form/clear-and-close form-id])}
       [tr :string.button/cancel]]
      [:button.button.primary {:type "submit"
                               :disabled (not (:source state))}
       [tr :string.button/save]]]]))

(rf/reg-event-fx ::show
  (fn [_ _]
    {:dispatch [::modal/create
                :string.user.avatar/title
                [form]
                #(rf/dispatch [::form/clear form-id])]}))
