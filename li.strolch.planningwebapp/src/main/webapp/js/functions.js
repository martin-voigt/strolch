/**
 * Created by eitch on 2015-09-04
 */


/*
 * Define the main namespace
 */
if (typeof strolch == 'undefined') {
    strolch = {};
}
strolch.fn = {};


/*
 * Helpers
 */
strolch.fn.url = function (subPath) {
    var path = strolch.const.url_base + '/' + subPath;
    return window.location.origin + '/' + path;
};

strolch.fn.translateI18n = function (locale) {
    if (locale == null || locale.length == 0 || locale == 'undefined') {
        console.log('Locale is \'' + locale + '\', detecting browser locale...');
        locale = i18n.detectLocale();
        console.log('Locale is now ' + locale);
    }

    i18n.path = 'localization';
    i18n.locale = locale;
    i18n.resource = 'localization';
    i18n.init(locale);
    i18n.translate_document();
    strolch.fn.setLocale(i18n.locale);
};

/*
 * configuration
 */
strolch.fn.getAuthToken = function () {
    return localStorage[strolch.const.auth_token];
};
strolch.fn.setAuthToken = function (authToken) {
    return localStorage[strolch.const.auth_token] = authToken;
};
strolch.fn.getSessionData = function () {
    var data = localStorage[strolch.const.session_data];
    if (data == null)
        return null;
    return JSON.parse(data);
};
strolch.fn.setSessionData = function (data) {
    if (data == null)
        throw "Data can not be null!";
    return localStorage[strolch.const.session_data] = JSON.stringify(data);
};
strolch.fn.clearSessionData = function () {
    localStorage.clear();
    sessionStorage.clear();
};

strolch.fn.getLocale = function () {
    return localStorage['language_locale'];
};
strolch.fn.setLocale = function (locale) {
    localStorage['language_locale'] = locale;
};

strolch.const.version = null;
strolch.fn.version = function () {

    if (strolch.const.version == null) {
        strolch.const.version = "unknown";
        $.ajax({
                async: false,
                url: strolch.fn.url(strolch.const.urls.version)
            }
        ).done(function (data) {
            if (data != null) {
                var ver = data.appVersion.artifactVersion;
                var rev = data.appVersion.scmRevision;
                if (rev == '${buildNumber}') {
                    strolch.const.version = ver;
                } else {
                    ver = ver ? ver.substr(0, 9) : '?';
                    rev = rev ? rev.substr(0, 7) : '?';
                    strolch.const.version = ver + " - " + rev;
                }
            }
        });
    }
    return strolch.const.version;
};

strolch.fn.revisionValue = null;
strolch.fn.revision = function () {

    if (strolch.fn.revisionValue == null) {
        strolch.fn.revisionValue = Math.floor(Math.random() * 10000000);
        $.ajax({
                async: false,
                url: strolch.fn.url(strolch.const.urls.version)
            }
        ).done(function (data) {
            if (data != null) {
                var rev = data.appVersion.scmRevision;
                if (rev != '${buildNumber}') {
                    strolch.fn.revisionValue = rev;
                }
            }
        });
    }
    return strolch.fn.revisionValue;
};


/*
 * Session management
 */
strolch.fn.showReAuthForm = function () {
    var reauthForm = $('#reauthFormModal');
    reauthForm.on('shown.bs.modal', function () {
        $('#auth-password').focus();
    });
    reauthForm.modal('show');
};
strolch.fn.reAuth = function () {

    var username = $('#auth-username').val();
    if (strolch.fn.isEmptyString(username)) {
        alertify.alert(i18n.t('auth.warning'), i18n.t('auth.username.empty'));
        return;
    }

    var password = $('#auth-password').val();
    if (strolch.fn.isEmptyString(password)) {
        alertify.alert(i18n.t('auth.warning'), i18n.t('auth.username.empty'));
        return;
    }

    console.log('Reauthenticating...');
    strolch.fn.doAuth('#reauthForm', username, password);
};

strolch.fn.doAuth = function (authFormSel, username, password) {

    var payload = JSON.stringify({
        username: username,
        password: btoa(password)
    });

    var url = strolch.fn.url(strolch.const.urls.auth);

    $.ajax({
        type: "POST",
        url: url,
        data: payload,
        contentType: 'application/json'
    }).success(function (data) {

        strolch.fn.toggleSubmitBtn(authFormSel);
        strolch.fn.setSessionData(data);
        strolch.fn.setAuthToken(data.authToken);
        console.log('Authenticated.');

        var authForm = $(authFormSel);
        var submitAction = authForm.data('submit-action');
        if (submitAction == 'navigate') {
            var navPage = $(authFormSel).data('submit-dst');
            var page = strolch.fn.url(navPage);
            console.log('User logged in, navigating to ' + page);
            window.location.href = page;
        } else if (submitAction == 'dismiss-modal') {
            var modalSel = $(authFormSel).data('submit-dst');
            strolch.fn.dismissModal(modalSel);
        } else if (submitAction == 'reload') {
            window.location.reload();
        } else {
            throw 'Unhandled submit-action ' + submitAction + ' on form ' + authFormSel;
        }


    }).fail(function (data) {

        strolch.fn.toggleSubmitBtn(authFormSel);

        if (data.status == 404) {
            alertify.alert(i18n.t('auth.failed'), i18n.t('auth.serverdown'));
        } else {
            var msg = data.responseJSON.msg;
            if (!msg)
                msg = data.statusText;
            alertify.alert(i18n.t('auth.failed'), data.status + ': ' + msg);
        }
    });
};

strolch.fn.logout = function () {

    var locale = strolch.fn.getLocale();
    strolch.fn.clearSessionData();
    strolch.fn.setLocale(locale);

    var url = strolch.fn.url('auth.html');
    console.log('Logging out and redirecting to ' + url);
    window.location.href = url;
};

strolch.fn.validateAuthToken = function () {

    var url = strolch.fn.url(strolch.const.urls.auth) + '/' + strolch.fn.getAuthToken();

    return $.ajax({
        type: "HEAD",
        url: url
    });
};


/*
 * Modal panel helpers
 */
strolch.fn.onModalShow = function (modalSelector, hideCallback) {

    var modal = $(modalSelector);
    modal.on('show.bs.modal', function (e) {
        modal.on('hide.bs.modal', function (e) {
            setTimeout(hideCallback, 5);
        });
        modal.find('button[data-dismiss=modal]').prop("disabled", false);
        modal.find('button[type=submit]').prop("disabled", false);
        modal.find('span[data-type=spinner]').hide();
    });
};

strolch.fn.onModalFormSubmit = function (modalSelector, formId, submitCallback) {
    var modal = $(modalSelector);
    $(formId).on('submit', function (e) {
        modal.unbind('hide.bs.modal').on('hide.bs.modal', function (e) {
            return false;
        });
        modal.find('button[data-dismiss=modal]').prop("disabled", true);
        modal.find('button[type=submit]').prop("disabled", true);
        modal.find('span[data-type=spinner]').show();

        setTimeout(submitCallback, 5);
    });
};

strolch.fn.dismissModal = function (modalSelector) {
    console.log('Dismissing modal ' + modalSelector);
    var modal = $(modalSelector);
    modal.unbind('hide.bs.modal').on('hide.bs.modal', function (e) {
        return true;
    });
    modal.modal('hide');
};

strolch.fn.toggleSubmitBtn = function (formId) {

    var form = $(formId);

    var submitBtn = form.find('button[type=submit]');
    submitBtn.prop("disabled", !submitBtn.prop("disabled"));

    var spinner = form.find('span[data-type=spinner]');
    if (spinner.is(':visible'))
        spinner.hide();
    else
        spinner.show();
};

/*
 * DataTable helpers
 */
strolch.fn.dataTableDefaults = function () {
    return {
        realmName: '',
        draw: 1,
        pageSize: 1,
        page: 0,
        query: '',
        orderBy: '',
        ascending: true,
        url: null,
        tableId: null,
        searchFieldId: null
    };
};
strolch.fn.initDataTable = function (queryData, columns, columnDefs) {

    var table = $('#' + queryData.tableId);
    if ($.fn.dataTable.isDataTable(table)) {
        table.DataTable().destroy();
        table.empty();
    }

    // init table
    table.dataTable({
        columns: columns,
        columnDefs: columnDefs,
        //lengthMenu: [[5, 10, 20, -1], [5, 10, 20, 'All']],
        lengthMenu: [[10, 50, 100, -1], [10, 50, 100, 'All']],
        processing: true,
        searching: false,
        deferLoading: 0,
        dom: 't<"bottom"iflp><"clear">',
        serverSide: true,
        ajax: function (data, callback, settings) {

            var payload = {
                realmName: queryData.realmName,
                draw: data.draw,
                pageSize: data.length,
                page: data.start / data.length + 1,
                query: queryData.query,
                orderBy: data.columns[data.order[0].column].data,
                ascending: data.order[0].dir == 'asc'
            };

            $.ajax({
                dataType: 'json',
                url: queryData.url,
                data: payload,
                success: function (data) {
                    data.recordsTotal = data.dataSetSize;
                    data.recordsFiltered = data.nrOfElements;
                    callback(data);
                }
            });
        }
    });

    table.find('tbody').on('click', 'tr', function () {
        var data = table.DataTable().row(this).data();
        $(this).toggleClass('selected').toggleClass('table-active');
        console.log('Selected ' + data['Id']);
    });
    table.find('tbody').on('click', 'tr span.row-action', function () {
        var rowE = $(this).closest('tr');
        var row = table.DataTable().row(rowE);
        var data = row.data();
        $('#jsonData').text(JSON.stringify(data, null, 2));
        $('#modelElementDetailsModal').modal('show');
    });

    strolch.fn.initSearch(queryData);
};

// this is for a search field belonging to a single table:
strolch.fn.initSearch = function (queryData) {

    var oldSearchValue = [null];

    $('#' + queryData.searchFieldId).keyup(function (e) {
        if (e.which !== 0) {
            var searchValue = $(this).val();
            if (oldSearchValue[0] == null || oldSearchValue[0] != searchValue) {
                oldSearchValue[0] = searchValue;
                queryData.query = searchValue;
                strolch.fn.searchDataTable(queryData);
            }
        }
    });
};
// this is the actual searching, which is set in a timeout, so we don't search too often and can cancel previous calls
strolch.fn.searchTimeouts = {};
strolch.fn.searchDataTable = function (queryData) {
    var minLength = 3;
    if (queryData.query.length == 0 || queryData.query.length >= minLength) {
        if (strolch.fn.searchTimeouts[queryData.searchFieldId] != null)
            clearTimeout(strolch.fn.searchTimeouts[queryData.searchFieldId]);
        strolch.fn.searchTimeouts[queryData.searchFieldId] = setTimeout(function () {
            $('#' + queryData.tableId).DataTable().draw();
        }, 300);
    }
};

/*
 * Utils
 */
strolch.fn.uuid = function () {
    var d = new Date().getTime();
    var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
        var r = (d + Math.random() * 16) % 16 | 0;
        d = Math.floor(d / 16);
        return (c == 'x' ? r : (r & 0x3 | 0x8)).toString(16);
    });
    return uuid;
};

strolch.fn.emptyString = function (val) {
    return strolch.fn.isEmptyString(val);
};

strolch.fn.isEmptyString = function (val) {
    return typeof val == 'undefined' || val == '';
};

strolch.fn.isFloat = function (val) {
    return Number(parseFloat(val)) == val;
};

strolch.fn.isInteger = function (val) {
    return Number(parseInt(val)) == val;
};

strolch.fn.isDate = function (val) {
    var pattern = /\\d\\d\\.\\d\\d\\.\\d\\d/;
    var isDate = pattern.test(val);
    return isDate;
};

strolch.fn.isTime = function (val) {
    var pattern = /[0-2][0-9]:[0-5][0-9]/;
    var isTime = pattern.test(val);
    return isTime;
};

strolch.fn.isEmail = function (val) {
    var pattern = /([a-zA-Z0-9_\-])([a-zA-Z0-9_\-\.]*)\+?([a-zA-Z0-9_\-])([a-zA-Z0-9_\-\.]*)?@(\[((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9][0-9]|[0-9])\.){3}|((([a-zA-Z0-9\-]+)\.)+))([a-zA-Z]{2,}|(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9][0-9]|[0-9])])/;
    var isEmail = pattern.test(val);
    return isEmail;
};

strolch.fn.equalsArray = function (a, b) {
    return $(a).not(b).length === 0 && $(b).not(a).length === 0;
};

strolch.fn.logException = function (e) {
    (console.error || console.log).call(console, e, e.stack || e);
};

strolch.fn.syntaxHighlightJson = function (json) {
    if (typeof json != 'string') {
        json = JSON.stringify(json, undefined, 2);
    }
    json = json.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    return json.replace(/("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g, function (match) {
        var cls = 'number';
        if (/^"/.test(match)) {
            if (/:$/.test(match)) {
                cls = 'key';
            } else {
                cls = 'string';
            }
        } else if (/true|false/.test(match)) {
            cls = 'boolean';
        } else if (/null/.test(match)) {
            cls = 'null';
        }
        return '<span class="' + cls + '">' + match + '</span>';
    });
};

/*
 * hack for multiple modals over each other - until bootstrap fixes this
 */
strolch.fn.multipleModalsHack = function () {

    var modal = $('.modal');

    modal.on('hidden.bs.modal', function (event) {
        $(this).removeClass('fv-modal-stack');
        var body = $('body');
        body.data('fv_open_modals', body.data('fv_open_modals') - 1);
    });

    modal.on('shown.bs.modal', function (event) {

        var body = $('body');

        // keep track of the number of open modals
        if (typeof( body.data('fv_open_modals') ) == 'undefined') {
            body.data('fv_open_modals', 0);
        }

        // if the z-index of this modal has been set, ignore.
        if ($(this).hasClass('fv-modal-stack')) {
            return;
        }

        $(this).addClass('fv-modal-stack');
        body.data('fv_open_modals', body.data('fv_open_modals') + 1);
        $(this).css('z-index', 1040 + (10 * body.data('fv_open_modals')));

        var modalBackDrop = $('.modal-backdrop');
        modalBackDrop.not('.fv-modal-stack').css('z-index', 1039 + (10 * body.data('fv_open_modals')));
        modalBackDrop.not('fv-modal-stack').addClass('fv-modal-stack');
    });
};
